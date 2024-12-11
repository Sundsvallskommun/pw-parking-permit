package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static apptest.mock.api.PartyAssets.mockPartyAssetsPost;
import static apptest.mock.api.Rpa.mockRpaAddQueueItems;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Execution {

	public static String mockExecution(String caseId, String scenarioName) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand");
		final var stateAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterCheckIfCardExists = mockExecutionCheckIfCardExists(caseId, scenarioName, stateAfterOrderCard);
		final var stateAfterCreateAsset = mockExecutionCreateAsset(caseId, scenarioName, stateAfterCheckIfCardExists);
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterCreateAsset);
	}

	public static String mockExecutionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Verkställa", "UNKNOWN", "ONGOING", "Verkställa")));
	}

	public static String mockExecutionOrderCard(String caseId, String scenarioName, String requiredScenarioState) {
		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_order-card-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"caseTypeParameter", "PARKING_PERMIT",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa"));

		final var stateAfterOrderCard = mockRpaAddQueueItems(scenarioName, stateAfterGetErrand,
			"execution_order-card-task-worker---api-rpa-post-queue-item",
			equalToJson("""
				{
					"itemData":
						{
					  		"Name": "${json-unit.any-string}",
					  		"SpecificContent":{},
					  		"Reference":"%s"
						}
				}
				""".formatted(caseId)));

		return mockCaseDataPutStatus(caseId, scenarioName, stateAfterOrderCard,
			"execution_update-phase-task-worker---api-casedata-put-errand",
			equalToJson("""
				[
					{
				    	"statusType": "Beslut verkställt",
				    	"description": "Beslut verkställt",
				    	"dateTime": "${json-unit.any-string}"
				  	}
				]
				"""));

	}

	public static String mockExecutionCreateAsset(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_create-asset-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));

		return mockPartyAssetsPost(scenarioName, state,
			"execution_create-asset-task-worker---api-party-assets-post-asset",
			equalToJson("""
				       {
							"partyId": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
						  	"assetId": "12345",
						  	"caseReferenceIds": [
								"%s"
						  	],
						  	"origin": "CASEDATA",
						  	"type": "PARKINGPERMIT",
						  	"issued": "2024-05-17",
						  	"validTo": "2025-05-17",
						  	"description": "Parkeringstillstånd",
						  	"additionalParameters": {}
					  }
				""".formatted(caseId)));
	}

	public static String mockExecutionCheckIfCardExists(String caseId, String scenarioName, String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));
	}

	public static String mockSendSimplifiedService(final String caseId, final String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_send-simplified-service-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		mockMessagingWebMessagePost(
			equalToJson("""
							{
								"party" : {
									"partyId" : "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
									"externalReferences" : [ {
										"key" : "flowInstanceId",
										"value" : "2971"
									} ]
				      			},
				      			"message" : "Kontrollmeddelande för förenklad delgivning\\n\\nVi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.\\nNär det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.\\nOm du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till\\nkontakt@sundsvall.se eller telefon till 060-19 10 00.",
				      			"oepInstance" : "external",
				      			"attachments" : [ ]
				    		}
				"""));
		return state;
	}
}
