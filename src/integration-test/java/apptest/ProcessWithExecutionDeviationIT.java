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
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Execution.mockExecutionCreateAsset;
import static apptest.mock.Execution.mockExecutionOrderCard;
import static apptest.mock.Execution.mockExecutionUpdatePhase;
import static apptest.mock.Execution.mockSendSimplifiedService;
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
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;

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

		final var caseId = "1415";
		final var scenarioName = "test_execution_001_createProcessForCardNotExistsToExists";

		//Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName);
		mockInvestigation(caseId, scenarioName);
		final var stateAfterDecision = mockDecision(caseId, scenarioName);
		// Mock Deviation
		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, stateAfterDecision);
		final var stateAfterOrderCard = mockExecutionOrderCard(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterCheckIfCardDoesNotExist = mockCaseDataGet(caseId, scenarioName, stateAfterOrderCard,
			"execution_check-if-card-exists-task-worker-when-it-does-not---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", ""));
		final var stateAfterCheckIfCardExists = mockCaseDataGet(caseId, scenarioName, stateAfterCheckIfCardDoesNotExist,
			"execution_check-if-card-exists-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Verkställa",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Verkställa",
				"permitNumberParameter", "12345"));
		final var stateAfterCreateAsset = mockExecutionCreateAsset(caseId, scenarioName, stateAfterCheckIfCardExists);
		mockSendSimplifiedService(caseId, scenarioName, stateAfterCreateAsset);
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
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			.with(actualizationPathway())
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(tuple("Execution", "call_activity_execution"))
			.with(tuple("Start execution phase", "start_execution_phase"))
			.with(tuple("Send message in parallel flow", "parallel_gateway_start"))
			.with(tuple("Update phase", "external_task_execution_update_phase"))
			.with(tuple("Gateway isAppeal", "execution_gateway_is_appeal"))
			.with(tuple("Order card", "external_task_execution_order_card_task"))
			.with(tuple("Check if card exists", "external_task_execution_check_if_card_exists"))
			// Card does not exist
			.with(tuple("Wait for existing card", "execution_card_check_is_update_available"))
			.with(tuple("Check if card exists", "external_task_execution_check_if_card_exists"))
			.with(tuple("Is card manufactured", "gateway_card_exists"))
			.with(tuple("Is card manufactured", "gateway_card_exists"))
			.with(tuple("Create asset", "external_task_execution_create_asset"))
			.with(tuple("End appeal", "execution_gateway_end_appeal"))
			//Added delay to send control message to make it happen after the asset is created
			.with(tuple("Wait to send message", "timer_wait_to_send_message"))
			.with(tuple("Send simplified service message", "external_task_execution_send_message_task"))
			.with(tuple("End parallel gateway", "parallel_gateway_end"))
			.with(tuple("End parallel gateway", "parallel_gateway_end"))
			.with(tuple("End execution phase", "end_execution_phase"))
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}
}
