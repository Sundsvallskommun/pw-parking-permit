package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataAddMessagePost;
import static apptest.mock.api.CaseData.mockCaseDataAddStakeholderPatch;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static apptest.mock.api.CaseData.mockCaseDataStakeholdersGet;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static apptest.mock.api.Templating.mockRenderPdf;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Denial {

	public static String mockDenial(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var stateAfterDenialUpdatePhase = mockDenialUpdatePhase(caseId, scenarioName, requiredScenarioState);
		final var stateAfterAddDecision = mockDenialAddDecision(caseId, scenarioName, stateAfterDenialUpdatePhase);
		final var stateAfterUpdateStatus = mockDenialUpdateStatus(caseId, scenarioName, stateAfterAddDecision);
		final var stateAfterSendDecision = mockDenialSendDecision(caseId, scenarioName, stateAfterUpdateStatus);
		return mockDenialAddMessageToErrand(caseId, scenarioName, stateAfterSendDecision);
	}

	public static String mockDenialUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"displayPhaseParameter", "Aktualisering"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Beslut", "UNKNOWN", "ONGOING", "Beslut")));
	}

	public static String mockDenialAddDecision(final String caseId, final String scenarioName, final String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"automatic-denial-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Beslut"));

		state = mockCaseDataAddStakeholderPatch(caseId, scenarioName, state,
			"automatic-denial-decision-task-worker---api-casedata-add-stakeholder-to-errand",
			equalToJson("""
					{
						"type": "PERSON",
						"firstName": "Process",
						"lastName": "Engine",
						"roles": [
							"ADMINISTRATOR"
						],
						"addresses": [],
						"contactInformation": [],
						"extraParameters": {}
					}
				"""));

		state = mockCaseDataStakeholdersGet(caseId, "2", scenarioName, state,
			"automatic-denial-decision-task-worker---api-casedata-get-stakeholder");

		state = mockRenderPdf(scenarioName, state, "add-message-to-errand-task-worker---api-templating-render-pdf",
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
			"automatic-denial-decision-task-worker---api-casedata-add-decision",
			equalToJson("""
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
								"attachments": [
									{
										"category": "BESLUT",
										"name": "beslut.pdf",
										"extension": "pdf",
										"mimeType": "application/pdf",
										"extraParameters": {},
										"file": "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4"
									}
								],
								"extraParameters": {}
							}
				"""));
	}

	public static String mockDenialUpdateStatus(final String caseId, final String scenarioName, final String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPutStatus(caseId, scenarioName, state,
			"update-status-task-worker---api-casedata-put-status",
			equalToJson("""
				[
					{
							"statusType": "Beslut verkställt",
						"description": "Ärendet avvisas",
						"dateTime": "${json-unit.any-string}"
					}
				]
				"""));
	}

	public static String mockDenialSendDecision(final String caseId, final String scenarioName, final String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"send-denial-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Beslut"));

		state = mockRenderPdf(scenarioName, state, "send-denial-decision-task-worker---api-templating-render-pdf",
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

		return mockMessagingWebMessagePost(scenarioName, state, "send-denial-decision-task-worker---api-messaging-send-web-message",
			equalToJson("""
							{
								"party": {
									"partyId": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
									"externalReferences": []
								},
								"message": "Ärendet avskrivs",
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

	public static String mockDenialAddMessageToErrand(final String caseId, final String scenarioName, final String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"add-message-to-errand-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Beslut"));

		state = mockRenderPdf(scenarioName, state, "add-message-to-errand-task-worker---api-templating-render-pdf",
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
			"add-message-to-errand-task-worker---api-post-message",
			equalToJson("""
							{
								"messageId": "570c3e28-b640-49e9-899c-9d290eb0539a",
								"errandNumber": "PRH-2022-000001",
								"direction": "OUTBOUND",
								"externalCaseId": "2971",
								"message": "Hej\\n\\nDu har fått ett beslut från Sundsvalls kommun.\\n\\nMed vänlig hälsning\\nSundsvalls kommun",
								"sent": "${json-unit.any-string}",
								"subject": "Beslut från Sundsvalls kommun",
								"username": "ProcessEngine",
								"attachmentRequests": [
									{
										"content": "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4",
										"name": "beslut.pdf",
										"contentType": "application/pdf"
									}
								],
								"emailHeaders": []
							}
				"""));
	}
}