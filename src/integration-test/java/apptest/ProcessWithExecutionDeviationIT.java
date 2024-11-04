package apptest;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;
import java.util.Map;

import static apptest.mock.Actualization.mockActualization;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Execution.mockExecutionCreateAsset;
import static apptest.mock.Execution.mockExecutionOrderCard;
import static apptest.mock.Execution.mockExecutionUpdatePhase;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.verification.ProcessPathway.actualizationPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static apptest.verification.ProcessPathway.handlingPathway;
import static apptest.verification.ProcessPathway.investigationPathway;
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

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
public class ProcessWithExecutionDeviationIT extends AbstractCamundaAppTest {

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
	void test_execution_001_createProcessForCardNotExistsToExists() throws JsonProcessingException, ClassNotFoundException {

		var caseId = "1415";
		var scenarioName = "test_execution_001_createProcessForCardNotExistsToExists";

		//Setup mocks
		mockApiGatewayToken();
		mockActualization(caseId, scenarioName);
		mockInvestigation(caseId, scenarioName);
		var scenarioAfterDecision = mockDecision(caseId, scenarioName);
		// Mock Deviation
		var scenarioAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, scenarioAfterDecision);
		var scenarioAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, scenarioAfterUpdatePhase);
		var scenarioAfterCheckIfCardDoesNotExist = mockCaseDataGet(caseId, scenarioName, scenarioAfterOrderCard,
			"execution_check-if-card-exists-task-worker-when-it-does-not---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", ""));
		var scenarioAfterCheckIfCardExists = mockCaseDataGet(caseId, scenarioName, scenarioAfterCheckIfCardDoesNotExist,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));
		mockExecutionCreateAsset(caseId, scenarioName, scenarioAfterCheckIfCardExists);
		// Normal mock
		mockFollowUp(caseId, scenarioName);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1415")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("execution_card_check_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Update process
		setupCall()
			.withServicePath("/2281/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), true, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(actualizationPathway())
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(tuple("Execution", "call_activity_execution"))
			.with(tuple("Start execution phase", "start_execution_phase"))
			.with(tuple("Update phase", "external_task_execution_update_phase"))
			.with(tuple("Order card", "external_task_execution_order_card_task"))
			.with(tuple("Check if card exists", "external_task_execution_check_if_card_exists"))
			// Card does not exist
			.with(tuple("Wait for existing card", "execution_card_check_is_update_available"))
			.with(tuple("Check if card exists", "external_task_execution_check_if_card_exists"))
			.with(tuple("Is card manufactured", "gateway_card_exists"))
			.with(tuple("Is card manufactured", "gateway_card_exists"))
			.with(tuple("Create Asset", "external_task_execution_create_asset"))
			.with(tuple("End execution phase", "end_execution_phase"))
			.with(followUpPathway())
			.with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
			.with(tuple("End process", "end_process")));
	}
}
