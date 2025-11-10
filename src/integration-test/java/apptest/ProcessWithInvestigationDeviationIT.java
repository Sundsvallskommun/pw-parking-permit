package apptest;

import static apptest.mock.Actualization.mockActualization;
import static apptest.mock.Canceled.mockCanceled;
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigationCheckPhaseAction;
import static apptest.mock.Investigation.mockInvestigationConstructDecision;
import static apptest.mock.Investigation.mockInvestigationExecuteRules;
import static apptest.mock.Investigation.mockInvestigationUpdatePhase;
import static apptest.mock.Investigation.mockInvestigationUpdateStatus;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.createPatchExtraParametersBody;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatchErrand;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Templating.mockRenderPdf;
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

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.core.JsonProcessingException;

import apptest.verification.Tuples;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessWithInvestigationDeviationIT extends AbstractCamundaAppTest {

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
	void test_investigation_002_createProcessForPhaseActionNotComplete() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1213";
		final var scenarioName = "test_investigation_002_createProcessForPhaseActionNotComplete";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		var state = mockActualization(caseId, scenarioName, false);

		// Mock deviation
		state = mockInvestigationUpdatePhase(caseId, scenarioName, state, false);
		state = mockInvestigationUpdateStatus(caseId, scenarioName, state, false);
		state = mockInvestigationExecuteRules(caseId, scenarioName, state, "-willNotComplete", true, false);
		state = mockInvestigationConstructDecision(caseId, scenarioName, state, "-willNotComplete", false);
		state = mockCaseDataGet(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-get-errand-willNotComplete",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Utredning"));
		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-errand-willNotComplete",
			equalToJson(createPatchBody("Utredning")));

		state = mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-extraparameters-willNotComplete",
			equalToJson(createPatchExtraParametersBody("UNKNOWN", "WAITING", "Utredning")),
			Map.of("phaseActionParameter", "UNKNOWN",
				"phaseStatusParameter", "WAITING",
				"displayPhaseParameter", "Utredning"));

		state = mockInvestigationExecuteRules(caseId, scenarioName, state, false);
		state = mockInvestigationConstructDecision(caseId, scenarioName, state, false);
		mockInvestigationCheckPhaseAction(caseId, scenarioName, state, false);

		// Normal mocks
		mockDecision(caseId, scenarioName, false);
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1213")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("investigation_phase_action_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
			// Investigation with deviation
			.with(tuple("Investigation", "investigation_phase"))
			.with(tuple("Start investigation phase", "start_investigation_phase"))
			.with(tuple("Update phase", "external_task_investigation_update_phase"))
			.with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
			.with(tuple("Execute rules", "external_task_investigation_execute_rules"))
			.with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_investigation_is_phase_action_complete_or_automatic"))
			// Phase action not complete
			.with(tuple("Wait for update", "investigation_phase_action_is_update_available"))
			.with(tuple("Execute rules", "external_task_investigation_execute_rules"))
			.with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_investigation_is_phase_action_complete_or_automatic"))
			.with(tuple("End investigation phase", "end_investigation_phase"))
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void test_investigation_003_createProcessForCancelInInvestigation(boolean isAutomatic) throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1314";
		var scenarioName = "test_investigation_003_createProcessForCancelInInvestigation";
		if (isAutomatic) {
			scenarioName = scenarioName.concat("_Automatic");
		}

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		var state = mockActualization(caseId, scenarioName, isAutomatic);

		// Mock deviation
		state = mockInvestigationUpdatePhase(caseId, scenarioName, state, isAutomatic);
		state = mockInvestigationUpdateStatus(caseId, scenarioName, state, isAutomatic);
		state = mockInvestigationExecuteRules(caseId, scenarioName, state, isAutomatic);
		state = mockInvestigationConstructDecision(caseId, scenarioName, state, isAutomatic);

		state = mockCaseDataGet(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Utredning"));

		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Utredning")));

		state = mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-extraparameters",
			equalToJson(createPatchExtraParametersBody("CANCEL", "CANCELED", "Utredning")),
			Map.of("phaseActionParameter", "CANCEL",
				"phaseStatusParameter", "CANCELED",
				"displayPhaseParameter", "Utredning"));

		mockCanceled(caseId, scenarioName, state);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1314")
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
			.with(actualizationPathway())
			.with(tuple("Gateway isCitizen", "gateway_is_citizen"))
			// Investigation with deviation
			.with(tuple("Investigation", "investigation_phase"))
			.with(tuple("Start investigation phase", "start_investigation_phase"))
			.with(tuple("Update phase", "external_task_investigation_update_phase"))
			.with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
			.with(tuple("Execute rules", "external_task_investigation_execute_rules"))
			.with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			// Phase action canceled
			.with(tuple("Is phase action complete or automatic?", "gateway_investigation_is_phase_action_complete_or_automatic"))
			.with(tuple("End when canceled", "end_investigation_canceled"))
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test_investigation_004_createProcessValidationErrorInBRToComplete() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1617";
		final var scenarioName = "test_investigation_004_createProcessValidationErrorInBRToComplete";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		var state = mockActualization(caseId, scenarioName, false);

		// Mock deviation
		state = mockInvestigationUpdatePhase(caseId, scenarioName, state, false);
		state = mockInvestigationUpdateStatus(caseId, scenarioName, state, false);
		// Returns validation error
		state = mockInvestigationExecuteRules(caseId, scenarioName, state, "willNotComplete", false, false);
		state = mockCaseDataGet(caseId, scenarioName, state,
			"construct-recommended-decision-task-worker-rejection---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Utredning"));
		state = mockCaseDataDecisionPatch(caseId, scenarioName, state,
			"investigation_execute-rules-task-worker-rejection---api-businessrules-engine",
			equalToJson("""
				{
				    "version": 2,
				    "created": "${json-unit.any-string}",
				    "decisionType": "RECOMMENDED",
				    "decisionOutcome": "REJECTION",
				    "description": "Rekommenderat beslut är avslag. Saknar giltigt värde för: 'disability.walkingDistance.max' (uppgift om maximal gångsträcka för den sökande).",
				    "law": [],
				    "attachments": [],
				    "extraParameters": {}
				}
				"""));
		// Will loop back and wait for update
		state = mockCaseDataGet(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-get-errand-willNotComplete",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Utredning"));
		state = mockCaseDataPatchErrand(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-errand-willNotComplete",
			equalToJson(createPatchBody("Utredning")));
		state = mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"investigation_check-phase-action_task-worker---api-casedata-patch-extraparameters-willNotComplete",
			equalToJson(createPatchExtraParametersBody("UNKNOWN", "WAITING", "Utredning")),
			Map.of("phaseActionParameter", "UNKNOWN",
				"phaseStatusParameter", "WAITING",
				"displayPhaseParameter", "Utredning"));

		// Passes on second attempt
		state = mockInvestigationExecuteRules(caseId, scenarioName, state, false);
		state = mockInvestigationConstructDecision(caseId, scenarioName, state, false);
		mockInvestigationCheckPhaseAction(caseId, scenarioName, state, false);

		// Normal mocks
		mockDecision(caseId, scenarioName, false);
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1617")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("investigation_phase_action_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
			// Investigation with deviation
			.with(tuple("Investigation", "investigation_phase"))
			.with(tuple("Start investigation phase", "start_investigation_phase"))
			.with(tuple("Update phase", "external_task_investigation_update_phase"))
			.with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
			.with(tuple("Execute rules", "external_task_investigation_execute_rules"))
			.with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_investigation_is_phase_action_complete_or_automatic"))
			// Phase action not complete
			.with(tuple("Wait for update", "investigation_phase_action_is_update_available"))
			.with(tuple("Execute rules", "external_task_investigation_execute_rules"))
			.with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			.with(tuple("Is phase action complete or automatic?", "gateway_investigation_is_phase_action_complete_or_automatic"))
			.with(tuple("End investigation phase", "end_investigation_phase"))
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test_investigation_005_createProcessValidationErrorInBRAutomatic() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1617";
		final var scenarioName = "test_investigation_005_createProcessValidationErrorInBRAutomatic";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARKING_PERMIT);
		final var stateAfterActualization = mockActualization(caseId, scenarioName, true);
		// Mock deviation
		final var stateAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, stateAfterActualization, true);
		final var stateAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, stateAfterUpdatePhase, true);
		// Returns validation error
		final var stateAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, stateAfterUpdateStatus, "willNotComplete", false, true);
		final var stateAfterConstructDecisionGet = mockCaseDataGet(caseId, scenarioName, stateAfterExecuteRules,
			"construct-recommended-decision-task-worker-rejection---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_AUTOMATIC,
				"displayPhaseParameter", "Utredning"));

		final var stateAfterRenderPdf = mockRenderPdf(scenarioName, stateAfterConstructDecisionGet, "investigation_construct-decision_task-worker---api-templating-render-pdf",
			equalToJson("""
							{
				                "identifier" : "sbk.rph.decision.driver.rejection.automatic",
				                "metadata" : [ ],
				                "parameters" : {
				                    "addressFirstname" : "John",
				                    "caseNumber" : "PRH-2022-000001",
				                    "addressLastname" : "Doe",
				                    "creationDate" : "2022-12-02",
				                    "decisionDate" : "${json-unit.any-string}"
				                }
				            }
				"""));
		final var stateAfterPatchDecision = mockCaseDataDecisionPatch(caseId, scenarioName, stateAfterRenderPdf,
			"investigation_execute-rules-task-worker-rejection---api-businessrules-engine",
			equalToJson("""
				{
				   "version" : 2,
				   "decisionType" : "FINAL",
				   "decisionOutcome" : "REJECTION",
				   "description" : "Beslut är avslag. Saknar giltigt värde för: 'disability.walkingDistance.max' (uppgift om maximal gångsträcka för den sökande).",
				   "law" : [ {
				     "heading" : "13 kap. 8§ Parkeringstillstånd för rörelsehindrade",
				     "sfs" : "Trafikförordningen (1998:1276)",
				     "chapter" : "13",
				     "article" : "8"
				   } ],
				   "decidedBy" : {
				     "id" : 1,
				     "version" : 0,
				     "type" : "PERSON",
				     "firstName" : "Kalle",
				     "lastName" : "Anka",
				     "personId" : "6b8928bb-9800-4d52-a9fa-20d88c812345",
				     "roles" : [ "ADMINISTRATOR" ],
				     "addresses" : [ {
				       "street" : "STORGATAN 1",
				       "postalCode" : "850 00",
				       "city" : "SUNDSVALL"
				     } ],
				     "contactInformation" : [ {
				       "contactType" : "PHONE",
				       "value" : "070-1740605"
				     }, {
				       "contactType" : "EMAIL",
				       "value" : "john.doe@example.com"
				     } ],
				     "extraParameters" : { },
				     "created" : "2022-12-02T15:13:45.371645+01:00",
				     "updated" : "2022-12-02T15:13:45.371676+01:00"
				   },
				   "decidedAt" : "${json-unit.any-string}",
				   "attachments" : [ {
				     "category" : "BESLUT",
				     "name" : "beslut.pdf",
				     "extension" : "pdf",
				     "mimeType" : "application/pdf",
				     "file" : "JVBERi0xLjcNCiW1tbW1DQoxIDAgb2JqDQo8PC9UeXBlL0NhdGFsb2cvUGFnZXMgMiAwIFIvTGFuZyhzdi1TRSkgL1N0cnVjdFRyZWVSb290IDE0IDAgUi9NYXJrSW5mbzw8L01hcmtlZCB0cnVlPj4vTWV0YWRhdGEgMjUgMCBSL1ZpZXdlclByZWZlcmVuY2VzIDI2IDAgUj4",
				     "extraParameters" : { }
				   } ],
				   "extraParameters" : { },
				   "created" : "${json-unit.any-string}"
				 }
				"""));

		final var stateAfterConstructDecision = mockCaseDataPatchStatus(caseId, scenarioName, stateAfterPatchDecision,
			"investigation_construct-decision_task-worker---api-casedata-patch-status-errand",
			equalToJson("""
				{
				    "statusType": "Beslutad",
				    "description": "Beslutad",
				    "created": "${json-unit.any-string}"
				}
				"""));

		mockInvestigationCheckPhaseAction(caseId, scenarioName, stateAfterConstructDecision, false);
		// Normal mocks
		mockDecision(caseId, scenarioName, false);
		mockExecution(caseId, scenarioName, false);
		mockFollowUp(caseId, scenarioName, false);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/1617")
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
			.with(actualizationPathway())
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
