package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Canceled {

    public static String mockCanceledInActualization(String caseId, String scenarioName, String requiredScenarioState) {
        final var stateAfterUpdatePhase = mockCanceledUpdatePhase(caseId, scenarioName, requiredScenarioState,  Map.of("decisionTypeParameter", "PROPOSED",
			"phaseParameter", "Aktualisering",
			"phaseStatusParameter", "ONGOING",
			"phaseActionParameter", "CANCEL",
			"displayPhaseParameter", "Granskning"));
		final var stateAfterUpdateStatus = mockCanceledUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase);
        return mockCanceledCleanUpNotes(caseId, scenarioName, stateAfterUpdateStatus);
    }

	public static String mockCanceledInInvestigation(String caseId, String scenarioName, String requiredScenarioState) {
		final var stateAfterUpdatePhase = mockCanceledUpdatePhase(caseId, scenarioName, requiredScenarioState, Map.of("decisionTypeParameter", "FINAL",
			"phaseParameter", "Utredning",
			"phaseStatusParameter", "ONGOING",
			"phaseActionParameter", "CANCEL",
			"displayPhaseParameter", "Utredning"));
		final var stateAfterUpdateStatus = mockCanceledUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase);
		return mockCanceledCleanUpNotes(caseId, scenarioName, stateAfterUpdateStatus);
	}

	public static String mockCanceledInDecision(String caseId, String scenarioName, String requiredScenarioState) {
		final var stateAfterUpdatePhase = mockCanceledUpdatePhase(caseId, scenarioName, requiredScenarioState,  Map.of("decisionTypeParameter", "FINAL",
			"phaseActionParameter", "CANCEL",
			"phaseParameter", "Beslut",
			"displayPhaseParameter", "Beslut",
			"statusTypeParameter", "Beslutad"));
		final var stateAfterUpdateStatus = mockCanceledUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase);
		return mockCanceledCleanUpNotes(caseId, scenarioName, stateAfterUpdateStatus);
	}

    public static String mockCanceledUpdatePhase(String caseId, String scenarioName, String requiredScenarioState,  Map<String, Object> transformParameters) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, "canceled_update-phase-task-worker---api-casedata-get-errand", transformParameters);

        return mockCaseDataPatch(caseId, scenarioName, state,
                "canceled_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Canceled","UNKNOWN", "ONGOING", "Avbruten")));
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

        return mockCaseDataPutStatus(caseId, scenarioName, state,
            "canceled_update-status-task-worker---api-casedata-put-status",
            equalToJson("""
				[
				  {
				    "statusType": "Ärende avslutat",
				    "description": "Processen har avbrutits",
				    "dateTime": "${json-unit.any-string}"
				  }
				]
				"""));
    }

    public static String mockCanceledCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
                "canceled_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");

        return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
                "canceled_update-phase-task-worker---api-casedata-delete-note");
    }

}