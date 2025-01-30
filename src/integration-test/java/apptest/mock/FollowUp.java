package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class FollowUp {

    public static String mockFollowUp(String caseId, String scenarioName) {
        return mockFollowUp(caseId, scenarioName, "execution_send-simplified-service-task-worker---api-casedata-get-errand");
    }

    public static String mockFollowUp(String caseId, String scenarioName, String requiredScenarioState) {
        final var stateAfterUpdatePhase = mockFollowUpUpdatePhase(caseId, scenarioName, requiredScenarioState);
        final var stateAfterCleanUp = mockFollowUpCleanUpNotes(caseId, scenarioName, stateAfterUpdatePhase);
        return mockFollowUpUpdateStatus(caseId, scenarioName, stateAfterCleanUp);
    }

    public static String mockFollowUpUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Beslut",
                        "displayPhaseParameter", "Beslut"));


        return mockCaseDataPatch(caseId, scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Uppföljning","UNKNOWN", "ONGOING", "Uppföljning")));
    }

    public static String mockFollowUpCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");


        return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-delete-note");

    }

    public static String mockFollowUpUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
            "follow_up_update-phase-task-worker---api-casedata-delete-note",
            Map.of("decisionTypeParameter", "FINAL",
                "phaseParameter", "Uppföljning",
                "phaseStatusParameter", "ONGOING",
                "phaseActionParameter", "UNKNOWN",
                "displayPhaseParameter", "Uppföljning"));

        return mockCaseDataPutStatus(caseId, scenarioName, state,
            "follow_up_update-errand-status---api-casedata-put-status",
            equalToJson("""
                        [
                          {
                            "statusType": "Ärende avslutat",
                            "description": "Ärende avslutat",
                            "dateTime": "${json-unit.any-string}"
                          }
                        ]
                        """));
    }

}