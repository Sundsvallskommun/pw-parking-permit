package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.*;
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
                equalToJson("""
                            {
                                "externalCaseId": "2971",
                                "phase": "Uppföljning",
                                 "extraParameters" : [
                                    {
                                        "key" : "process.phaseStatus",
                                        "values" : [ "ONGOING" ]
                                    },
                                    {
                                        "key" : "process.phaseAction",
                                        "values" : [ "UNKNOWN" ]
                                    },
                                    {
                                        "key" : "process.displayPhase",
                                        "values" : [ "Uppföljning" ]
                                    }]
                            }
                            """));
    }

    public static String mockFollowUpCleanUpNotes(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataNotesGet(caseId, scenarioName, requiredScenarioState,
                "follow_up_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");


        return mockCaseDataNotesDelete(caseId, "128", scenarioName, state,
                "follow_up_update-phase-task-worker---api-casedata-delete-note");

    }

}