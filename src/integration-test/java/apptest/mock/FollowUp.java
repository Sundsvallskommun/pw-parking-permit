package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.getPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class FollowUp {

    public static String mockFollowUp(String caseId, String scenarioName) {
        return mockFollowUp(caseId, scenarioName, "execution_create-asset-task-worker---api-party-assets-post-asset");
    }

    public static String mockFollowUp(String caseId, String scenarioName, String requiredScenarioState) {
        var scenarioAfterUpdatePhase = mockFollowUpUpdatePhase(caseId, scenarioName, requiredScenarioState);
        return mockFollowUpCleanUpNotes(caseId, scenarioName, scenarioAfterUpdatePhase);
    }

    public static String mockFollowUpUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Beslut",
                        "displayPhaseParameter", "Beslut"));


        return mockCaseDataPatch(caseId, scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson(getPatchBody("Uppföljning","UNKNOWN", "ONGOING", "Uppföljning")));
    }

    public static String mockFollowUpCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");


        return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-delete-note");

    }

}