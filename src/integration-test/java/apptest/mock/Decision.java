package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.createPatchExtraParametersBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatchErrand;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Decision {

	public static String mockDecision(String caseId, String scenarioName, boolean isAutomatic) {
		final var scenarioAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, "investigation_check-phase-action_task-worker---api-casedata-patch-extraparameters", isAutomatic);
		final var scenarioAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase, isAutomatic);
		return mockDecisionCheckIfDecisionMade(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockDecisionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"phaseStatusParameter", "ONGOING",
				"displayPhaseParameter", "Utredning"));

		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"decision_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Beslut")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"decision_update_phase-task-worker---api-casedata-patch-extraparameters",
			equalToJson(createPatchExtraParametersBody(isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Beslut")),
			Map.of("phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"phaseStatusParameter", "ONGOING",
				"displayPhaseParameter", "Beslut"));
	}

	public static String mockDecisionUpdateStatus(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"decision_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under beslut",
				    "description": "Ärendet beslutas",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockDecisionCheckIfDecisionMade(String caseId, String scenarioName, String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"check-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));
	}

	public static String mockDecisionHandling(String municipalityId, String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		final var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"decision_decision-handling-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"decision_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under beslut",
				    "description": "Ärendet beslutas",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}
}
