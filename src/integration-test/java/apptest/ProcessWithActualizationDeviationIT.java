package apptest;

import apptest.mock.Actualization;
import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;

import static apptest.mock.Actualization.mockActualizationUpdatePhase;
import static apptest.mock.Actualization.mockActualizationVerifyResident;
import static apptest.mock.Denial.mockDenial;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.verification.ProcessPathway.denialPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/WireMock/", classes = Application.class)
class ProcessWithActualizationDeviationIT extends AbstractCamundaAppTest {

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

		final var caseId = "456";
		final var scenarioName = "test_actualization_001_createProcessForNonCitizen";
		//Setup mocks
		mockApiGatewayToken();
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, STARTED);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "other-municipality");
		final var stateAfterDenial = mockDenial(caseId, scenarioName, "verify-resident-of-municipality-task-worker---api-citizen-getcitizen");
		mockFollowUp(caseId, scenarioName, stateAfterDenial);

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
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
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
