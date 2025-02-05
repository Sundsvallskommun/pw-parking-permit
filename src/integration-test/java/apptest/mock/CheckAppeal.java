package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.mockCaseDataGet;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class CheckAppeal {

	public static String mockCheckAppeal(String caseId, String scenarioName, String caseType) {
		return mockCheckAppealGetErrand(caseId, scenarioName, STARTED, caseType);
	}

	public static String mockCheckAppealGetErrand(String caseId, String scenarioName, String requiredScenarioState, String caseType) {

		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"check_appeal_check-appeal-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "",
				"caseTypeParameter", caseType));
	}
}
