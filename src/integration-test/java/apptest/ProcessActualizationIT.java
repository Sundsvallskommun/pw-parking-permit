package apptest;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;

import static apptest.mock.Denial.mockDenial;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.verification.ProcessPathway.denialPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

@WireMockAppTestSuite(files = "classpath:/ProcessActualization/", classes = Application.class)
class ProcessActualizationIT extends AbstractCamundaAppTest {

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
	void test001_createProcessForNonCitizen() throws JsonProcessingException, ClassNotFoundException {

		//Setup mocks
		final var stateAfterDenial = mockDenial("456", "create-process-for-non-citizen", "verify-resident-of-municipality-task-worker---api-citizen-getcitizen");
		mockFollowUp("456", "create-process-for-non-citizen", stateAfterDenial);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/456")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), 999);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), Tuples.create()
			.with(tuple("Start process", "start_process"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("End when not citizen of municipality", "end_actualization_not_citizen"))
			.with(denialPathway())
			.with(followUpPathway())
			.with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
			.with(tuple("End process", "end_process")));
	}
}
