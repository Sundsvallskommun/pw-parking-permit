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
        final var stateAfterUpdatePhase = mockFollowUpUpdatePhaseAtStart(caseId, scenarioName, requiredScenarioState);
        final var stateAfterCheckPhaseAction = mockFollowUpCheckPhaseAction(caseId, scenarioName, stateAfterUpdatePhase);
        final var stateAfterCleanUp = mockFollowUpCleanUpNotes(caseId, scenarioName, stateAfterCheckPhaseAction);
        final var stateAfterUpdateStatus = mockFollowUpUpdateStatus(caseId, scenarioName, stateAfterCleanUp);
        return mockFollowUpUpdatePhaseAtEnd(caseId, scenarioName, stateAfterUpdateStatus);
    }

    public static String mockFollowUpCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
            "follow_up_check-phase-action_task-worker---api-casedata-get-errand-check-phase-action",
            Map.of("decisionTypeParameter", "FINAL",
                "phaseParameter", "Uppföljning",
                "phaseStatusParameter", "ONGOING",
                "statusTypeParameter", "Status",
                "phaseActionParameter", "COMPLETE",
                "displayPhaseParameter", "Uppföljning"));

        return mockCaseDataPatch(caseId, scenarioName, state,
            "follow_up_check-phase-action_task-worker---api-casedata-patch-errand-start",
            equalToJson(createPatchBody("Uppföljning", "COMPLETE", "COMPLETED", "Uppföljning")));
    }

    public static String mockFollowUpUpdatePhaseAtStart(String caseId, String scenarioName, String requiredScenarioState) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-errand-update-phase-start",
            Map.of("decisionTypeParameter", "FINAL",
                "phaseParameter", "Beslut",
                "phaseStatusParameter", "ONGOING",
                "statusTypeParameter", "Status",
                "phaseActionParameter", "UNKNOWN",
                "displayPhaseParameter", "Beslut"));


        return mockCaseDataPatch(caseId, scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Uppföljning", "UNKNOWN", "ONGOING", "Uppföljning")));
    }

    public static String mockFollowUpUpdatePhaseAtEnd(String caseId, String scenarioName, String requiredScenarioState) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
            "follow_up_update-phase-task-worker---api-casedata-get-errand-update-phase-end",
            Map.of("decisionTypeParameter", "FINAL",
                "phaseParameter", "Uppföljning",
                "phaseStatusParameter", "COMPLETED",
                "statusTypeParameter", "Ärende avslutat",
                "phaseActionParameter", "UNKNOWN",
                "displayPhaseParameter", "Uppföljning"));


        return mockCaseDataPatch(caseId, scenarioName, state,
            "follow_up_update-phase-task-worker---api-casedata-patch-errand-end",
            equalToJson(createPatchBody("Uppföljning", "UNKNOWN", "COMPLETED", "Uppföljning")));
    }

    public static String mockFollowUpCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");


        return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-delete-note");

    }

    public static String mockFollowUpUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
            "follow_up_update-phase-task-worker---api-casedata-get-errand-update-status",
            Map.of("decisionTypeParameter", "FINAL",
                "phaseParameter", "Uppföljning",
                "phaseStatusParameter", "ONGOING",
                "phaseActionParameter", "UNKNOWN",
                "statusTypeParameter", "Status",
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