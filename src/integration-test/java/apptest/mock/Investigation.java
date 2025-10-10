package apptest.mock;

import java.util.Map;

import static apptest.mock.api.BusinessRules.mockBusinessRulesPost;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataGetAttachments;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Templating.mockRenderPdf;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

public class Investigation {

	public static String mockInvestigation(String caseId, String scenarioName, boolean isAutomatic) {
		var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, "actualization_check-phase-action_task-worker---api-casedata-patch-errand", isAutomatic);
		var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase, isAutomatic);
		var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterUpdateStatus, isAutomatic);
		var scenarioAfterConstructDecision = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules, isAutomatic);
		return mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterConstructDecision, isAutomatic);
	}

	public static String mockInvestigationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"investigation_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "COMPLETE",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Aktualisering"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"investigation_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Utredning", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Utredning")));
	}

	public static String mockInvestigationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"investigation_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Utredning"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"investigation_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under utredning",
				    "description": "Ärendet utreds",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockInvestigationExecuteRules(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		return mockInvestigationExecuteRules(caseId, scenarioName, requiredScenarioState, null, true, isAutomatic);
	}

	public static String mockInvestigationExecuteRules(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix, boolean validResponse, boolean isAutomatic) {
		var newScenarioStateGet = "investigation_execute-rules-task-worker---api-casedata-get-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
		}
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
			Map.of("decisionTypeParameter", "FINAL",
				"caseTypeParameter", "PARKING_PERMIT",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Utredning"));

		var newScenarioStateAttachment = "investigation_execute-rules-task-worker---api-case-data-get-errand-attachments";
		if (newScenarioStateSuffix != null) {
			newScenarioStateAttachment = newScenarioStateAttachment.concat(newScenarioStateSuffix);
		}
		var stateAfterAttachment = mockCaseDataGetAttachments(caseId, scenarioName, state, newScenarioStateAttachment);

		var newScenarioStatePost = "investigation_execute-rules-task-worker---api-businessrules-engine";
		if (newScenarioStateSuffix != null) {
			newScenarioStatePost = newScenarioStatePost.concat(newScenarioStateSuffix);
		}

		return mockBusinessRulesPost(scenarioName, stateAfterAttachment, newScenarioStatePost,
			equalToJson("""
				{
				    "context": "PARKING_PERMIT",
				    "facts": [
				        {
				            "key": "type",
				            "value": "PARKING_PERMIT"
				        },
				        {
				            "key": "stakeholders.applicant.personid",
				            "value": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6"
				        },
				        {
				            "key": "application.applicant.capacity",
				            "value": "DRIVER"
				        },
				        {
				            "key": "disability.duration",
				            "value": "P1Y"
				        },
				        {
				            "key": "disability.walkingAbility",
				            "value": "false"
				        },
				        {
				            "key": "disability.canBeAloneWhileParking",
				            "value": "true"
				        },
				        {
				            "key": "application.applicant.signingAbility",
				            "value": "false"
				        },
				        {
				            "key": "attachment.medicalConfirmation",
				            "value": "true"
				        },
				        {
				            "key": "attachment.passportPhoto",
				            "value": "false"
				        },
				        {
				            "key": "attachment.signature",
				            "value": "false"
				        }
				    ]
				}
				"""),
			Map.of(),
			validResponse);
	}

	public static String mockInvestigationConstructDecision(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		return mockInvestigationConstructDecision(caseId, scenarioName, requiredScenarioState, null, isAutomatic);
	}

	public static String mockInvestigationConstructDecision(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix, boolean isAutomatic) {
		var newScenarioStateGet = "construct-recommended-decision-task-worker---api-casedata-get-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
		}
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Utredning"));

		var newScenarioStatePatch = "investigation_construct-recommended-decision_task-worker---api-casedata-patch-decision";
		if (newScenarioStateSuffix != null) {
			newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
		}

		var stateAfterPatchDecision = "";
		if (isAutomatic) {
			state = mockRenderPdf(scenarioName, state, "investigation_construct-recommended-decision_task-worker---api-templating-render-pdf",
				equalToJson("""
							{
								"identifier": "sbk.rph.decision.driver.approval",
								"metadata": [],
								"parameters": {
									"addressFirstname": "John",
									"caseNumber": "PRH-2022-000001",
									"addressLastname": "Doe",
									"creationDate": "2022-12-02",
									"decisionDate": "${json-unit.any-string}"
				    			}
				    		}
				"""));

			stateAfterPatchDecision = mockCaseDataDecisionPatch(caseId, scenarioName, state, newScenarioStatePatch,
				equalToJson("""
					{
				        "version": 2,
				        "created": "${json-unit.any-string}",
				        "decisionType": "FINAL",
				        "decisionOutcome": "APPROVAL",
				        "description": "Beslut är bevilja. Den sökande är helt rullstolsburen, funktionsnedsättningens varaktighet är 6 månader eller längre och den sökande har inga aktiva parkeringstillstånd.",
				        "law" : [ {
							"heading" : "13 kap. 8§ Parkeringstillstånd för rörelsehindrade",
							"sfs" : "Trafikförordningen (1998:1276)",
							"chapter" : "13",
					        "article" : "8"
						} ],
						"decidedBy" : {
					        "id" : 1,
					        "version" : 0,
					        "type" : "PERSON",
					        "firstName" : "Kalle",
					        "lastName" : "Anka",
					        "personId" : "6b8928bb-9800-4d52-a9fa-20d88c812345",
					        "roles" : [ "ADMINISTRATOR" ],
					        "addresses" : [ {
					            "street" : "STORGATAN 1",
					            "postalCode" : "850 00",
					            "city" : "SUNDSVALL"
							} ],
							"contactInformation" : [ {
								"contactType" : "PHONE",
					            "value" : "070-1740605"
					            }, {
					            "contactType" : "EMAIL",
					            "value" : "john.doe@example.com"
					        } ],
					        "extraParameters" : { },
					        "created" : "2022-12-02T15:13:45.371645+01:00",
					        "updated" : "2022-12-02T15:13:45.371676+01:00"
						},
						"decidedAt" : "${json-unit.any-string}",
						"validFrom" : "${json-unit.any-string}",
						"validTo" : "${json-unit.any-string}",
						"attachments" : [ {
							"category" : "BESLUT",
							"name" : "beslut.pdf",
							"extension" : "pdf",
					        "mimeType" : "application/pdf",
					        "file" : "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4",
					        "extraParameters" : { }
						} ],
						"extraParameters" : { }
					}
				"""));
		} else {
			stateAfterPatchDecision = mockCaseDataDecisionPatch(caseId, scenarioName, state, newScenarioStatePatch,
				equalToJson("""
				{
				    "version": 2,
				    "created": "${json-unit.any-string}",
				    "decisionType": "RECOMMENDED",
				    "decisionOutcome": "APPROVAL",
				    "description": "Rekommenderat beslut är bevilja. Den sökande är helt rullstolsburen, funktionsnedsättningens varaktighet är 6 månader eller längre och den sökande har inga aktiva parkeringstillstånd.",
				    "law": [],
				    "attachments": [],
				    "extraParameters": {}
				}
				"""));
		}

		if (isAutomatic) {
			return mockCaseDataPatchStatus(caseId, scenarioName, stateAfterPatchDecision,
				"investigation_construct-recommended-decision_task-worker---api-casedata-patch-status",
				equalToJson("""
					{
					    "statusType": "Beslutad",
					    "description": "Beslutad",
					    "created": "${json-unit.any-string}"
					}
					"""));
		} else {
			return stateAfterPatchDecision;
		}
	}

	public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		return mockInvestigationCheckPhaseAction(caseId, scenarioName, requiredScenarioState, null, isAutomatic);
	}

	public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix, boolean isAutomatic) {
		var newScenarioStateGet = "investigation_check-phase-action_task-worker---api-casedata-get-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
		}
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Utredning"));

		var newScenarioStatePatch = "investigation_check-phase-action_task-worker---api-casedata-patch-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
		}
		return mockCaseDataPatch(caseId, scenarioName, state, newScenarioStatePatch,
			equalToJson(createPatchBody("Utredning", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE, "COMPLETED", "Utredning")));
	}
}
