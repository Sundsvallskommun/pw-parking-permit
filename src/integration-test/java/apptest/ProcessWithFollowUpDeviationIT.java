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
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUpCheckPhaseAction;
import static apptest.mock.FollowUp.mockFollowUpCleanUpNotes;
import static apptest.mock.FollowUp.mockFollowUpUpdatePhaseAtEnd;
import static apptest.mock.FollowUp.mockFollowUpUpdatePhaseAtStart;
import static apptest.mock.FollowUp.mockFollowUpUpdateStatus;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.verification.ProcessPathway.actualizationPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.executionPathway;
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

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessWithFollowUpDeviationIT extends AbstractCamundaAppTest {

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
	void test001_createProcessForFollowUpNotComplete() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1011";
		final var scenarioName = "test_actualization_001_createProcessForFollowUpNotComplete";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		mockActualization(caseId, scenarioName);
		mockInvestigation(caseId, scenarioName);
		mockDecision(caseId, scenarioName);
		final var stateAfterExcecution = mockExecution(caseId, scenarioName);

		final var stateAfterUpdatePhase = mockFollowUpUpdatePhaseAtStart(caseId, scenarioName, stateAfterExcecution);

		final var stateAfterGetErrandNonComplete = mockCaseDataGet(caseId, scenarioName, stateAfterUpdatePhase,
			"follow_up_check-phase-action_task-worker---api-casedata-get-errand-non-complete",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Uppföljning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Uppföljning"));

		final var stateAfterPatchNonComplete = mockCaseDataPatch(caseId, scenarioName, stateAfterGetErrandNonComplete,
			"follow_up_check-phase-action_task-worker---api-casedata-patch-errand-non-complete",
			equalToJson(createPatchBody("Uppföljning", "UNKNOWN", "WAITING", "Uppföljning"), true, false));

		final var stateAfterCheckPhaseAction = mockFollowUpCheckPhaseAction(caseId, scenarioName, stateAfterPatchNonComplete);
		final var stateAfterCleanup = mockFollowUpCleanUpNotes(caseId, scenarioName, stateAfterCheckPhaseAction);
		final var stateAfterUpdateStatus = mockFollowUpUpdateStatus(caseId, scenarioName, stateAfterCleanup);
		mockFollowUpUpdatePhaseAtEnd(caseId, scenarioName, stateAfterUpdateStatus);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("followup_is_case_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			// Follow up pathway
			.with(tuple("Follow up", "call_activity_follow_up"))
			.with(tuple("Start follow up phase", "start_follow_up_phase"))
			.with(tuple("Update phase", "external_task_follow_up_update_phase"))
			.with(tuple("Check phase action", "external_task_followup_check_phase_action"))
			.with(tuple("Is phase action complete or automatic?", "gateway_followup_is_phase_action_complete_or_automatic"))
			// Not complete
			.with(tuple("Is caseUpdateAvailable", "followup_is_case_update_available"))
			.with(tuple("Check phase action", "external_task_followup_check_phase_action"))
			.with(tuple("Is phase action complete or automatic?", "gateway_followup_is_phase_action_complete_or_automatic"))
			.with(tuple("Clean up notes", "external_task_follow_up_clean_up_notes"))
			.with(tuple("Update errand status", "external_task_follow_up_update_status"))
			.with(tuple("Update phase action", "external_task_follow_up_update_phase_action"))
			.with(tuple("End follow up phase", "end_follow_up_phase"))
			.with(tuple("End process", "end_process")));
	}

}
