package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.createPatchExtraParametersBody;
import static apptest.mock.api.CaseData.mockCaseDataAddMessagePost;
import static apptest.mock.api.CaseData.mockCaseDataAddStakeholderPatch;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatchErrand;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.CaseData.mockCaseDataStakeholdersGet;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static apptest.mock.api.Templating.mockRenderPdf;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Denial {

	public static String mockDenial(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {
		final var stateAfterDenialUpdatePhase = mockDenialUpdatePhase(caseId, scenarioName, requiredScenarioState, isAutomatic);
		final var stateAfterAddDecision = mockDenialAddDecision(caseId, scenarioName, stateAfterDenialUpdatePhase, isAutomatic);
		final var stateAfterUpdateStatus = mockDenialUpdateStatus(caseId, scenarioName, stateAfterAddDecision, isAutomatic);
		final var stateAfterSendDecision = mockDenialSendDecision(caseId, scenarioName, stateAfterUpdateStatus, isAutomatic);
		final var stateAfterAddedMessageToErrand = mockDenialAddMessageToErrand(caseId, scenarioName, stateAfterSendDecision, isAutomatic);
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterAddedMessageToErrand);
	}

	public static String mockDenialUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"displayPhaseParameter", "Aktualisering",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN));

		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"automatic_denial_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Beslut")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"automatic_denial_update-phase-task-worker---api-casedata-patch-extraparameters",
			equalToJson(createPatchExtraParametersBody(isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Beslut")),
			Map.of("phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"phaseStatusParameter", "ONGOING",
				"displayPhaseParameter", "Beslut"));
	}

	public static String mockDenialAddDecision(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		state = mockCaseDataAddStakeholderPatch(caseId, scenarioName, state,
			"automatic_denial_decision-task-worker---api-casedata-add-stakeholder-to-errand",
			equalToJson("""
					{
						"type": "PERSON",
						"firstName": "Process",
						"lastName": "Engine",
						"roles": [
							"ADMINISTRATOR"
						]
					}
				"""));

		state = mockCaseDataStakeholdersGet(caseId, "2", scenarioName, state,
			"automatic_denial_decision-task-worker---api-casedata-get-stakeholder");

		state = mockRenderPdf(scenarioName, state, "automatic_denial_add-message-to-errand-task-worker---api-templating-render-pdf",
			equalToJson("""
							{
								"identifier": "sbk.prh.decision.all.rejection.municipality",
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

		return mockCaseDataDecisionPatch(caseId, scenarioName, state,
			"automatic_denial_decision-task-worker---api-casedata-add-decision",
			equalToJson(
				"""
								{
									"created": "${json-unit.any-string}",
									"decisionType": "FINAL",
									"decisionOutcome": "DISMISSAL",
									"description": "Personen inte folkbokförd i Sundsvalls kommun.",
									"law": [
										{
											"heading": "13 kap. 8§ Parkeringstillstånd för rörelsehindrade",
											"sfs": "Trafikförordningen (1998:1276)",
											"chapter": "13",
											"article": "8"
										}
									],
									"decidedBy": {
										"id": 2,
										"version": 2,
										"created": "2022-12-02T15:18:45.363499+01:00",
										"updated": "2022-12-02T15:19:01.5636+01:00",
										"type": "PERSON",
										"firstName": "Process",
										"lastName": "Engine",
										"roles": [
											"ADMINISTRATOR"
										],
										"addresses": [],
										"contactInformation": [],
										"extraParameters": {}
									},
									"decidedAt": "${json-unit.any-string}",
									"attachments": [
										{
											"category": "BESLUT",
											"name": "beslut.pdf",
											"extension": "pdf",
											"mimeType": "application/pdf",
											"file": "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4"
										}
									]
								}
					"""));
	}

	public static String mockDenialUpdateStatus(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"automatic_denial_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
					{
							"statusType": "Beslut verkställt",
						"description": "Ärendet avvisas",
						"created": "${json-unit.any-string}"
					}
				"""));
	}

	public static String mockDenialSendDecision(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_send-denial-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		state = mockRenderPdf(scenarioName, state, "automatic_denial_send-denial-decision-task-worker---api-templating-render-pdf",
			equalToJson("""
							{
								"identifier": "sbk.prh.decision.all.rejection.municipality",
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

		return mockMessagingWebMessagePost(scenarioName, state, "automatic-denial_send-denial-decision-task-worker---api-messaging-send-web-message",
			equalToJson(
				"""
								{
									"party": {
										"partyId": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
										"externalReferences" : [ {
					                              "key" : "flowInstanceId",
					                              "value" : "2971"
					                          } ]
									},
									"message": "Ärendet avskrivs",
									"sendAsOwner" : false,
					                "oepInstance" : "EXTERNAL",
									"attachments": [
										{
											"fileName": "beslut.pdf",
											"mimeType": "application/pdf",
											"base64Data": "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4"
										}
									]
								}
					"""));
	}

	public static String mockDenialAddMessageToErrand(final String caseId, final String scenarioName, final String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_add-message-to-errand-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		state = mockRenderPdf(scenarioName, state, "automatic_denial_add-message-to-errand-task-worker---api-templating-render-pdf",
			equalToJson("""
							{
								"identifier": "sbk.prh.decision.all.rejection.municipality",
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

		return mockCaseDataAddMessagePost(caseId, scenarioName, state,
			"automatic_denial_add-message-to-errand-task-worker---api-post-message",
			equalToJson(
				"""
								{
									"messageId": "570c3e28-b640-49e9-899c-9d290eb0539a",
									"direction": "OUTBOUND",
									"externalCaseId": "2971",
									"message": "Hej\\n\\nDu har fått ett beslut från Sundsvalls kommun.\\n\\nMed vänlig hälsning\\nSundsvalls kommun",
									"sent": "${json-unit.any-string}",
									"subject": "Beslut från Sundsvalls kommun",
									"username": "ProcessEngine",
									"attachments": [
										{
											"content": "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4",
											"name": "beslut.pdf",
											"contentType": "application/pdf"
										}
									]
								}
					"""));
	}

	public static String mockSendSimplifiedService(final String caseId, final String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic_denial_send-simplified-service-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		mockMessagingWebMessagePost(
			equalToJson(
				"""
								{
									"party" : {
										"partyId" : "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
										"externalReferences" : [ {
											"key" : "flowInstanceId",
											"value" : "2971"
										} ]
					      			},
					      			"message" : "Kontrollmeddelande för förenklad delgivning\\n\\nVi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.\\nNär det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.\\nOm du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till\\nkontakt@sundsvall.se eller telefon till 060-19 10 00.",
					                "sendAsOwner" : false,
					                "oepInstance" : "EXTERNAL"
					    		}
					"""));
		return state;
	}
}
