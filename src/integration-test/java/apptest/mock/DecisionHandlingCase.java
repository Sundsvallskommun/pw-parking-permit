package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static apptest.mock.api.PartyAssets.mockPartyAssetsPost;
import static apptest.mock.api.Rpa.mockRpaAddQueueItems;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class DecisionHandlingCase {

	public static String mockExecution(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(municipalityId, caseId, scenarioName, requiredScenarioState, isAutomatic);
		final var stateAfterHandleLostCard = mockExecutionHandleLostCard(municipalityId, caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterOrderCard = mockExecutionOrderCard(municipalityId, caseId, scenarioName, stateAfterHandleLostCard, isAutomatic);
		final var stateAfterCheckIfCardExists = mockExecutionCheckIfCardExists(municipalityId, caseId, scenarioName, stateAfterOrderCard, isAutomatic);
		final var stateAfterCreateAsset = mockExecutionCreateAsset(municipalityId, caseId, scenarioName, stateAfterCheckIfCardExists, isAutomatic);
		return mockSendSimplifiedService(municipalityId, caseId, scenarioName, stateAfterCreateAsset);
	}

	public static String mockExecutionUpdatePhase(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {

		final var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));

		return mockCaseDataPatch(municipalityId, caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Verkställa", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Verkställa")));
	}

	public static String mockExecutionHandleLostCard(final String muncipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {

		return mockCaseDataGet(muncipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_handle-lost-card-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : "",
				"caseTypeParameter", CASE_TYPE_PARKING_PERMIT,
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Verkställa",
				"statusTypeParameter", "Beslut verkställt"));
	}

	public static String mockExecutionOrderCard(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {
		final var stateAfterGetErrand = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_order-card-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"caseTypeParameter", "PARKING_PERMIT",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
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
				}f
				""".formatted(caseId)));

		return mockCaseDataPatchStatus(municipalityId, caseId, scenarioName, stateAfterOrderCard,
			"execution_update-phase-task-worker---api-casedata-patch-status",
			equalToJson("""
					{
				    	"statusType": "Beslut verkställt",
				    	"description": "Beslut verkställt",
				    	"created": "${json-unit.any-string}"
				  	}
				"""));

	}

	public static String mockExecutionCreateAsset(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {
		final var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_create-asset-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));

		return mockPartyAssetsPost(scenarioName, municipalityId, state,
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

	public static String mockExecutionCheckIfCardExists(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {
		return mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));
	}

	public static String mockSendSimplifiedService(final String municipalityId, final String caseId, final String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"execution_send-simplified-service-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		mockMessagingWebMessagePost(municipalityId,
			equalToJson("""
							{
								"party" : {
									"partyId" : "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
									"externalReferences" : [ {
										"key" : "flowInstanceId",
										"value" : "2971"
									} ]
				      			},
				      			"message" : "Kontrollmeddelande för förenklad delgivning\\n\\nVi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.\\nNär det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.\\nOm du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till\\nange@ange.se eller telefon till 0690-25 01 00.",
				      			"sendAsOwner" : false,
				                "oepInstance" : "EXTERNAL"
				    		}
				"""));
		return state;
	}
}
