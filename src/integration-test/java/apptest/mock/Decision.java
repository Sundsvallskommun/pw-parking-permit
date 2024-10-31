package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Decision {

	public static String mockDecision(String caseId, String scenarioName) {
		var scenarioAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, "investigation_check-phase-action_task-worker---api-casedata-patch-errand");
		var scenarioAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
		return mockDecisionCheckIfDecisionMade(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockDecisionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-phase-task-worker---api-casedata-patch-errand",
			Map.of("decisionTypeParameter", "FINAL",
					"phaseParameter", "Utredning",
				"phaseActionParameter", "ONGOING",
				"phaseStatusParameter", "UNKNOWN",
				"displayPhaseParameter", "Utredning"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"decision_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson("""
				{
				    "externalCaseId": "2971",
				    "phase": "Beslut",
				    "extraParameters": {
				        "process.phaseStatus": "ONGOING",
				        "process.phaseAction": "UNKNOWN",
				        "process.displayPhase": "Beslut"
				    }
				}
				"""));
	}

	public static String mockDecisionUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
					"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPutStatus(caseId, scenarioName, state,
			"decision_update-status-task-worker---api-casedata-put-status",
			equalToJson("""
				[
				  {
				    "statusType": "Under beslut",
				    "description": "Ärendet beslutas",
				    "dateTime": "${json-unit.any-string}"
				  }
				]
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
}