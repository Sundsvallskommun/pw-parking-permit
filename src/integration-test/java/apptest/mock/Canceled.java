package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

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

		return mockCaseDataPatch(caseId, scenarioName, state,
			"canceled_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Canceled", "UNKNOWN", "ONGOING", "Avbruten")));
	}

	public static String mockCanceledUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
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
		var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
			"canceled_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");

		return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
			"canceled_update-phase-task-worker---api-casedata-delete-note");
	}

}
