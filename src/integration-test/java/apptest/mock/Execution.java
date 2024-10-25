package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static apptest.mock.api.PartyAssets.mockPartyAssetsPost;
import static apptest.mock.api.Rpa.mockRpaAddQueueItems;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Execution {

	public static String mockExecution(String caseId, String scenarioName) {
		var scenarioAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand");
		var scenarioAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, scenarioAfterUpdatePhase);
		var scenarioAfterCheckIfCardExists = mockExecutionCheckIfCardExists(caseId, scenarioName, scenarioAfterOrderCard);
		return mockExecutionCreateAsset(caseId, scenarioName, scenarioAfterCheckIfCardExists);
	}

	public static String mockExecutionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-phase-task-worker---api-casedata-get-errand",
			Map.of("phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson("""
				{
				    "externalCaseId": "2971",
				    "phase": "Verkställa",
				    "extraParameters": {
				        "process.phaseStatus": "ONGOING",
				        "process.phaseAction": "UNKNOWN",
				        "process.displayPhase": "Verkställa"
				    }
				}
				"""));
	}

	public static String mockExecutionOrderCard(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_order-card-task-worker---api-casedata-get-errand",
			Map.of(
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa"));

		state = mockRpaAddQueueItems(scenarioName, state,
			"execution_order-card-task-worker---api-rpa-post-queue-item",
			equalToJson("""
				{
					"itemData":
						{
					  		"Name": "${json-unit.any-string}",
					  		"SpecificContent":{},
					  		"Reference":"123"
						}
				}
				"""));

		return mockCaseDataPutStatus(caseId, scenarioName, state,
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
			Map.of("phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa"));

		return mockPartyAssetsPost(scenarioName, state,
			"execution_create-asset-task-worker---api-party-assets-post-asset",
			equalToJson("""
				       {
							"partyId": "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
						  	"assetId": "12345",
						  	"caseReferenceIds": [
						    	"123"
						  	],
						  	"origin": "CASEDATA",
						  	"type": "PARKINGPERMIT",
						  	"issued": "2024-05-17",
						  	"validTo": "2025-05-17",
						  	"description": "Parkeringstillstånd",
						  	"additionalParameters": {}
					  }
				"""));
	}

	public static String mockExecutionCheckIfCardExists(String caseId, String scenarioName, String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa"));
	}
}