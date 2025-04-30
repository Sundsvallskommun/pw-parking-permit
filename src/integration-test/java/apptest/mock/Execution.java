package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.createPatchBodyWhenLostCard;
import static apptest.mock.api.CaseData.mockCaseDataAddNotePatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static apptest.mock.api.PartyAssets.mockPartyAssetsGet;
import static apptest.mock.api.PartyAssets.mockPartyAssetsGetByPartyIdAndStatus;
import static apptest.mock.api.PartyAssets.mockPartyAssetsPost;
import static apptest.mock.api.PartyAssets.mockPartyAssetsPut;
import static apptest.mock.api.Rpa.mockRpaAddQueueItems;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Execution {

	public static String mockExecution(String caseId, String scenarioName, boolean isAutomatic) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand", isAutomatic);
		final var stateAfterHandleLostCard = mockExecutionHandleLostCard(caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, stateAfterHandleLostCard, isAutomatic);
		final var stateAfterCheckIfCardExists = mockExecutionCheckIfCardExists(caseId, scenarioName, stateAfterOrderCard, isAutomatic);
		final var stateAfterCreateAsset = mockExecutionCreateAsset(caseId, scenarioName, stateAfterCheckIfCardExists, isAutomatic);
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterCreateAsset);
	}

	public static String mockExecutionWhenLostCard(String caseId, String scenarioName, boolean isAutomatic) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand", isAutomatic);
		final var stateAfterHandleLostCard = mockExecutionHandleLostCardWhenLost(caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, stateAfterHandleLostCard, isAutomatic);
		final var stateAfterCheckIfCardExists = mockExecutionCheckIfCardExists(caseId, scenarioName, stateAfterOrderCard, isAutomatic);
		final var stateAfterCreateAsset = mockExecutionCreateAsset(caseId, scenarioName, stateAfterCheckIfCardExists, isAutomatic);
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterCreateAsset);
	}

	public static String mockExecutionWhenAppeal(String caseId, String scenarioName, boolean isAutomatic) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand", isAutomatic);
		final var stateAfterUpdateAsset = mockExecutionUpdateAsset(caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterUpdateAsset);
	}

	public static String mockExecutionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {

		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Verkställa", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Verkställa")));
	}

	public static String mockExecutionHandleLostCard(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {

		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_handle-lost-card-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : "",
				"caseTypeParameter", CASE_TYPE_PARKING_PERMIT,
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));
	}

	public static String mockExecutionHandleLostCardWhenLost(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {

		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_handle-lost-card-worker-when-lost---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"caseTypeParameter", CASE_TYPE_LOST_PARKING_PERMIT,
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		final var stateAfterGetAssets = mockPartyAssetsGetByPartyIdAndStatus(scenarioName, stateAfterGetErrand,
			"execution_handle-lost-card-task-worker---api-party-assets-get-assets", "6b8928bb-9800-4d52-a9fa-20d88c81f1d6", "ACTIVE");

		final var stateAfterPutAsset = mockPartyAssetsPut("1c8f38a6-b492-4037-b7dc-de5bc6c629f0", scenarioName, stateAfterGetAssets,
			"execution_handle-lost-card-task-worker---api-party-asset-put-asset",
			equalToJson("""
				{
					"caseReferenceIds":[],
					"status":"BLOCKED",
					"statusReason":"LOST",
					"additionalParameters":{}
				}
				"""));

		final var stateAfterPatchErrand = mockCaseDataPatch(caseId, scenarioName, stateAfterPutAsset,
			"execution_handle-lost-card-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBodyWhenLostCard(isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "", "Beslut", "12345")));

		return mockCaseDataAddNotePatch(caseId, scenarioName, stateAfterPatchErrand,
			"execution_handle-lost-card-task-worker---api-casedata-add-note",
			equalToJson("""
				{
				   "municipalityId" : "2281",
				   "namespace" : "SBK_PARKING_PERMIT",
				   "title" : "Asset blocked",
				   "text" : "The asset with id 1c8f38a6-b492-4037-b7dc-de5bc6c629f0 has been blocked.",
				   "noteType" : "PUBLIC",
				   "extraParameters" : { }
				 }
				"""));
	}

	public static String mockExecutionOrderCard(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
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
				}
				""".formatted(caseId)));

		return mockCaseDataPatchStatus(caseId, scenarioName, stateAfterOrderCard,
			"execution_update-phase-task-worker---api-casedata-patch-status",
			equalToJson("""
					{
				    	"statusType": "Beslut verkställt",
				    	"description": "Beslut verkställt",
				    	"created": "${json-unit.any-string}"
				  	}
				"""));

	}

	public static String mockExecutionCreateAsset(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_create-asset-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
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

	public static String mockExecutionUpdateAsset(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_update-asset-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));

		final var stateAfterGetAppealedErrand = mockCaseDataGet("456", scenarioName, stateAfterGetErrand,
			"execution_update-asset-task-worker---api-casedata-get-appealed_errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));

		final var stateAfterGetAssets = mockPartyAssetsGet(scenarioName, stateAfterGetAppealedErrand,
			"execution_update-asset-task-worker---api-party-assets-get-errand", "12345", "6b8928bb-9800-4d52-a9fa-20d88c81f1d6", "ACTIVE");

		return mockPartyAssetsPut("1c8f38a6-b492-4037-b7dc-de5bc6c629f0", scenarioName, stateAfterGetAssets,
			"execution_update-asset-task-worker---api-party-asset-put-asset",
			equalToJson("""
				{
					"caseReferenceIds" : [ ],
					"additionalParameters" : {
						"foo" : "bar",
						"appealedErrand" : "123"
					}
				}
				"""));

	}

	public static String mockExecutionCheckIfCardExists(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
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
