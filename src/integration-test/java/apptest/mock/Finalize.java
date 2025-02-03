package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Finalize {

    public static String mockFinalize(String caseId, String scenarioName) {
        return mockFinalizeSetCheckPhaseAction(caseId, scenarioName, "follow_up_update-errand-status---api-casedata-put-status");
    }

    public static String mockFinalizeSetCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, "finalize-process-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Uppföljning",
                        "phaseStatusParameter", "COMPLETED",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Uppföljning"));

        return mockCaseDataPatch(caseId, scenarioName, state, "finalize-process-task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Uppföljning","UNKNOWN", "COMPLETED", "Uppföljning")));
    }
}
