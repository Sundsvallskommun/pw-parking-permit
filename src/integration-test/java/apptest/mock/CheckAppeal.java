package apptest.mock;

import java.time.OffsetDateTime;
import java.util.Map;

import static apptest.mock.api.CaseData.mockCaseDataGet;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class CheckAppeal {

	public static String mockCheckAppeal(String caseId, String scenarioName, String caseType) {
		if ("APPEAL".equals(caseType)) {
			return mockCheckAppealGetAppealErrands(caseId, scenarioName, STARTED, caseType);
		}
		return mockCheckAppealGetErrand(caseId, scenarioName, STARTED, caseType);
	}

	public static String mockCheckAppealGetAppealErrands(String caseId, String scenarioName, String requiredScenarioState, String caseType) {

		final var stateAfterGetAppeal = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"check_appeal_check-appeal-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "",
				"decidedAtParameter", "2024-12-24T08:31:29.181Z",
				"applicationReceivedParameter", "2024-01-01T15:17:01.563Z",
				"caseTypeParameter", caseType));

		return mockCaseDataGet("456", scenarioName, stateAfterGetAppeal,
			"check_appeal_check-appeal-task-worker---api-casedata-get-appealed_errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "",
				"decidedAtParameter", "2024-12-24T08:31:29.181Z",
				"applicationReceivedParameter", "2024-01-01T15:17:01.563Z",
				"caseTypeParameter", "PARKING_PERMIT"));

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
