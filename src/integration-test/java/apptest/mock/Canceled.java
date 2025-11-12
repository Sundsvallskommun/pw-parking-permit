package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.createPatchExtraParametersBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatchErrand;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

import java.util.Map;

public class Canceled {

	public static String mockCanceled(String caseId, String scenarioName, String requiredScenarioState) {
		final var stateAfterUpdatePhase = mockCanceledUpdatePhase(caseId, scenarioName, requiredScenarioState);
		final var stateAfterUpdateStatus = mockCanceledUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase);
		return mockCanceledCleanUpNotes(caseId, scenarioName, stateAfterUpdateStatus);
	}

	public static String mockCanceledUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, "canceled_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "PhaseBeforeCancel",
				"phaseStatusParameter", "CANCELED",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "DisplayPhaseBeforeCancel"));

		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"canceled_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Canceled")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"canceled_update_phase-task-worker---api-casedata-patch-extraparameters",
			equalToJson(createPatchExtraParametersBody("UNKNOWN", "ONGOING", "Avbruten")),
			Map.of("phaseActionParameter", "UNKNOWN",
				"phaseStatusParameter", "ONGOING",
				"displayPhaseParameter", "Avbruten"));
	}

	public static String mockCanceledUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"canceled_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Canceled",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Avbruten"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"canceled_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Ärende avslutat",
				    "description": "Processen har avbrutits",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockCanceledCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
			"canceled_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");

		return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
			"canceled_update-phase-task-worker---api-casedata-delete-note");
	}

}
