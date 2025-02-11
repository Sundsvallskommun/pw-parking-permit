package apptest;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import static apptest.mock.Actualization.mockActualizationCheckPhaseAction;
import static apptest.mock.Actualization.mockActualizationUpdateDisplayPhase;
import static apptest.mock.Actualization.mockActualizationUpdatePhase;
import static apptest.mock.Actualization.mockActualizationUpdateStatus;
import static apptest.mock.Actualization.mockActualizationVerifyAdministratorStakeholder;
import static apptest.mock.Actualization.mockActualizationVerifyResident;
import static apptest.mock.Canceled.mockCanceled;
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Denial.mockDenial;
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.verification.ProcessPathway.canceledPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.denialPathway;
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
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
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

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void test001_createProcessForNonCitizen(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "456";
		var scenarioName = "test_actualization_001_createProcessForNonCitizen";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal, isAutomatic);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "other-municipality", isAutomatic);
		final var stateAfterDenial = mockDenial(caseId, scenarioName, stateAfterVerifyResident, isAutomatic);
		mockFollowUp(caseId, scenarioName, stateAfterDenial, isAutomatic);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
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
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("End when not citizen of municipality", "end_actualization_not_citizen"))
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(denialPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void test002_createProcessForCancelInActualization(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "789";
		var scenarioName = "test_actualization_002_createProcessForCancelInActualization";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal, isAutomatic);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "2281", isAutomatic);
		final var stateAfterVerifyStakeholder = mockActualizationVerifyAdministratorStakeholder(caseId, scenarioName, stateAfterVerifyResident, isAutomatic);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder, isAutomatic);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase, isAutomatic);

		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Granskning"));

		final var stateAfterPatchErrand = mockCaseDataPatch(caseId, scenarioName, stateAfterGetErrand,
			"actualization_check-phase-action_task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", "CANCEL", "CANCELED", "Granskning"), true, false));

		mockCanceled(caseId, scenarioName, stateAfterPatchErrand);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
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
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_actualization_is_phase_action_complete_or_automatic"))
			.with(tuple("End when canceled", "end_actualization_canceled"))
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			// Canceled
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test003_createProcessForActualizationNotComplete() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1011";
		final var scenarioName = "test_actualization_003_createProcessForActualizationNotComplete";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal, false);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "2281", false);
		final var stateAfterVerifyStakeholder = mockActualizationVerifyAdministratorStakeholder(caseId, scenarioName, stateAfterVerifyResident, false);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder, false);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase, false);

		final var stateAfterGetErrandNonComplete = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand-non-complete",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Granskning"));

		final var stateAfterPatchNonComplete = mockCaseDataPatch(caseId, scenarioName, stateAfterGetErrandNonComplete,
			"actualization_check-phase-action_task-worker---api-casedata-patch-errand-non-complete",
			equalToJson(createPatchBody("Aktualisering", "UNKNOWN", "WAITING", "Granskning"), true, false));

		mockActualizationCheckPhaseAction(caseId, scenarioName, stateAfterPatchNonComplete, false);

		// Normal mock
		mockInvestigation(caseId, scenarioName, false);
		mockDecision(caseId, scenarioName, false);
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("actualization_is_case_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_actualization_is_phase_action_complete_or_automatic"))
			// phase action is not complete
			.with(tuple("Wait for complete action", "actualization_is_case_update_available"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_actualization_is_phase_action_complete_or_automatic"))
			.with(tuple("End actualization phase", "end_actualization_phase"))
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void test004_createProcessForCancelInActualizationWhenVerifyingAdministrator(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1920";
		var scenarioName = "test_actualization_004_createProcessForCancelInActualizationWhenVerifyingAdministrator";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT_RENEWAL);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal, isAutomatic);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "2281", isAutomatic);
		final var stateAfterGetCancelInVerifyStakeholder = mockCaseDataGet(caseId, scenarioName, stateAfterVerifyResident,
			"actualization_verify-administrator-stakeholder--api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Registrerad"));

		final var stateAfterPatchErrand = mockCaseDataPatch(caseId, scenarioName, stateAfterGetCancelInVerifyStakeholder,
			"actualization_verify-administrator-stakeholder--api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", "CANCEL", "CANCELED", "Registrerad"), true, false));

		mockCanceled(caseId, scenarioName, stateAfterPatchErrand);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
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
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
			.with(tuple("End when canceled", "end_actualization_canceled"))
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			// Canceled
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test005_createProcessWaitingForStakeholderUpdateInActualization() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "2021";
		final var scenarioName = "test_actualization_005_createProcessWaitingForStakeholderUpdateInActualization(";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_LOST_PARKING_PERMIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal, false);
		final var stateAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, stateAfterUpdatePhase, "2281", false);
		final var stateAfterVerifyStakeholderNoAdministrator = mockCaseDataGet(caseId, scenarioName, stateAfterVerifyResident,
			"actualization_verify-administrator-stakeholder---api-casedata-get-errand-no-administrator",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Registrerad"), "APPROVAL", "DOCTOR");

		final var stateAfterPatchNoAdministrator = mockCaseDataPatch(caseId, scenarioName, stateAfterVerifyStakeholderNoAdministrator,
			"actualization_verify-administrator-stakeholder--api-casedata-patch-errand-no-administrator",
			equalToJson(createPatchBody("Aktualisering", "UNKNOWN", "WAITING", "Registrerad"), true, false));

		final var stateAfterVerifyStakeholder = mockActualizationVerifyAdministratorStakeholder(caseId, scenarioName, stateAfterPatchNoAdministrator, false);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder, false);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase, false);

		mockActualizationCheckPhaseAction(caseId, scenarioName, stateAfterUpdateStatus, false);

		// Normal mock
		mockInvestigation(caseId, scenarioName, false);
		mockDecision(caseId, scenarioName, false);
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("actualization_wait_for_stakeholder_update", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
			// Stakeholder Administrator is not assigned
			.with(tuple("Wait for case update", "actualization_wait_for_stakeholder_update"))
			// Stakeholder update
			.with(tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_actualization_is_phase_action_complete_or_automatic"))
			.with(tuple("End actualization phase", "end_actualization_phase"))
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

}
