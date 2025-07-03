package apptest;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import generated.se.sundsvall.camunda.VariableValueDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;
import java.util.Map;

import static apptest.mock.Actualization.mockActualization;
import static apptest.mock.Canceled.mockCanceled;
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecisionCheckIfDecisionMade;
import static apptest.mock.Decision.mockDecisionUpdatePhase;
import static apptest.mock.Decision.mockDecisionUpdateStatus;
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataGetWithMunicipalityId;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.verification.ProcessPathway.actualizationPathway;
import static apptest.verification.ProcessPathway.canceledPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.executionPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static apptest.verification.ProcessPathway.handlingPathway;
import static apptest.verification.ProcessPathway.investigationPathway;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
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
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessWithDecisionDeviationIT extends AbstractCamundaAppTest {

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
	void test_decision_001_createProcessForDecisionNotFinalToFinal() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "789";
		final var scenarioName = "test_decision_001_createProcessForDecisionNotFinalToFinal";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName, false);
		final var stateAfterInvestigation = mockInvestigation(caseId, scenarioName, false);
		// Mock deviation
		final var stateAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, stateAfterInvestigation, false);
		final var stateAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase, false);
		final var stateAfterCheckDecisionNonFinalGet = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"check-decision-task-worker-not-final---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));
		final var stateAfterCheckDecisionNonFinalPatch = mockCaseDataPatch(caseId, scenarioName, stateAfterCheckDecisionNonFinalGet,
			"check-decision-task-worker-not-final---api-casedata-patch-errand",
			equalToJson(createPatchBody("Beslut", "UNKNOWN", "WAITING", "Beslut")));
		mockDecisionCheckIfDecisionMade(caseId, scenarioName, stateAfterCheckDecisionNonFinalPatch);
		// Normal mock
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/789")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("decision_is_case_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Update process
		setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/update/" + startResponse.getProcessId())
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
			.with(tuple("Decision", "decision_phase"))
			.with(tuple("Start decision phase", "start_decision_phase"))
			.with(tuple("Update phase on errand", "external_task_decision_update_phase"))
			.with(tuple("Update errand status", "external_task_decision_update_errand_status"))
			.with(tuple("Check if decision is made", "external_task_check_decision_task"))
			.with(tuple("Gateway is decision final", "gateway_is_decision_final"))
			// Decision not final
			.with(tuple("Is caseUpdateAvailable", "decision_is_case_update_available"))
			.with(tuple("Check if decision is made", "external_task_check_decision_task"))
			// Decision final
			.with(tuple("Gateway is decision final", "gateway_is_decision_final"))
			.with(tuple("End decision phase", "end_decision_phase"))
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void test_decision_002_createProcessForCancelDecision(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {
		final var caseId = "1516";
		var scenarioName = "test_decision_002_createProcessForCancelDecision";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName, isAutomatic);
		final var stateAfterInvestigation = mockInvestigation(caseId, scenarioName, isAutomatic);
		// Mock deviation
		final var stateAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, stateAfterInvestigation, isAutomatic);
		final var stateAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"check-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseActionParameter", "CANCEL",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));

		mockCanceled(caseId, scenarioName, stateAfterGetErrand);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1516")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			.with(actualizationPathway())
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void test_decision_003_createProcessForDecisionRejected(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {
		final var caseId = "1718";
		var scenarioName = "test_decision_003_createProcessForDecisionRejected";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName, isAutomatic);
		final var stateAfterInvestigation = mockInvestigation(caseId, scenarioName, isAutomatic);
		// Mock deviation
		final var stateAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, stateAfterInvestigation, isAutomatic);
		final var stateAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterCheckDecision = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"check-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"),
			"REJECTION");
		// Normal mocks
		mockFollowUp(caseId, scenarioName, stateAfterCheckDecision, isAutomatic);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1718")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			.with(actualizationPathway())
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

/*	@Test
	void test_decision_004_createProcessForDecisionAnge() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "789";
		final var scenarioName = "test_decision_004_createProcessForDecisionAnge";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName, false);
		final var stateAfterInvestigation = mockInvestigation(caseId, scenarioName, false);
		// Mock deviation
		final var stateAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, stateAfterInvestigation, false);
		final var stateAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase, false);
		final var stateAfterCheckDecisionNonFinalGet = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"check-decision-task-worker-not-final---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));
		final var stateAfterCheckDecisionNonFinalPatch = mockCaseDataPatch(caseId, scenarioName, stateAfterCheckDecisionNonFinalGet,
			"check-decision-task-worker-not-final---api-casedata-patch-errand",
			equalToJson(createPatchBody("Beslut", "UNKNOWN", "WAITING", "Beslut")));
		final var stateAfterCheckDecisionFinal = mockCaseDataGetWithMunicipalityId(caseId, scenarioName, stateAfterCheckDecisionNonFinalPatch,
			"check-decision-task-worker-not-final---api-casedata-get-errand-municipality",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"),
			"APPROVAL", "ADMINISTRATOR", "2260");
		// Normal mock
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/789")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("decision_is_case_update_available", 999);

		// Set municipalityId to 2260
		camundaClient.setProcessInstanceVariable(startResponse.getProcessId(), "municipalityId", new VariableValueDto()
			.type("String")
			.value("2260"));
		// Update process
		setupCall()
			.withServicePath("/2260/SBK_PARKING_PERMIT/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		awaitProcessState("start_execution_phase", 999);
		// Reset municipalityId to 2281
		camundaClient.setProcessInstanceVariable(startResponse.getProcessId(), "municipalityId", new VariableValueDto()
			.type("String")
			.value("2281"));
		camundaClient.setProcessInstanceVariable(startResponse.getProcessId(), "suspended", new VariableValueDto()
			.type("Boolean")
			.value("false"));


		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), 999);

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
			.with(tuple("Decision", "decision_phase"))
			.with(tuple("Start decision phase", "start_decision_phase"))
			.with(tuple("Update phase on errand", "external_task_decision_update_phase"))
			.with(tuple("Update errand status", "external_task_decision_update_errand_status"))
			.with(tuple("Check if decision is made", "external_task_check_decision_task"))
			.with(tuple("Gateway is decision final", "gateway_is_decision_final"))
			// Decision not final
			.with(tuple("Is caseUpdateAvailable", "decision_is_case_update_available"))
			.with(tuple("Check if decision is made", "external_task_check_decision_task"))
			// Decision final
			.with(tuple("Gateway is decision final", "gateway_is_decision_final"))
			.with(tuple("End decision phase", "end_decision_phase"))
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}*/

}
