package apptest;

import static apptest.mock.Denial.mockDenial;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.verification.ProcessPathway.denialPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessAppealDeviationIT extends AbstractCamundaAppTest {

	private static final int DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS = 30;
	private static final String TENANT_ID_PARKING_PERMIT = "PARKING_PERMIT";

	@BeforeEach
	void setup() {
		setDefaultPollInterval(500, MILLISECONDS);
		setDefaultPollDelay(ZERO);
		setDefaultTimeout(Duration.ofSeconds(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS));

		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getDeployments(null, null, TENANT_ID_PARKING_PERMIT).size(), equalTo(1));
	}

	@Test
	void test001_createProcessForAppealWhenNotInTime() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "123";
		final var scenarioName = "test_appeal_001_createProcessForAppealWhenNotInTime";
		// Setup mocks
		mockApiGatewayToken();

		final var stateAfterGetAppeal = mockCaseDataGet(caseId, scenarioName, STARTED,
			"check_appeal_check-appeal-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "",
				"decidedAtParameter", "2024-12-01T08:31:29.181Z",
				// Appeal is received a month after the decision is made
				"applicationReceivedParameter", "2025-01-01T15:17:01.563Z",
				"caseTypeParameter", CASE_TYPE_APPEAL));

		final var stateAfterGetAppealedErrand = mockCaseDataGet("456", scenarioName, stateAfterGetAppeal,
			"check_appeal_check-appeal-task-worker---api-casedata-get-appealed_errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "",
				// Decision is made a month before the appeal is received
				"decidedAtParameter", "2024-12-01T08:31:29.181Z",
				"applicationReceivedParameter", "2024-01-01T15:17:01.563Z",
				"caseTypeParameter", CASE_TYPE_PARKING_PERMIT));

		final var stateAfterDenial = mockDenial(caseId, scenarioName, stateAfterGetAppealedErrand);
		mockFollowUp(caseId, scenarioName, stateAfterDenial);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), true, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			.with(denialPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}
}
