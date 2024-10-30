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
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigationCheckPhaseAction;
import static apptest.mock.Investigation.mockInvestigationConstructDecision;
import static apptest.mock.Investigation.mockInvestigationExecuteRules;
import static apptest.mock.Investigation.mockInvestigationSanityChecks;
import static apptest.mock.Investigation.mockInvestigationUpdatePhase;
import static apptest.mock.Investigation.mockInvestigationUpdateStatus;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataDecisionPatch;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.verification.ProcessPathway.actualizationPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.executionPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static apptest.verification.ProcessPathway.handlingPathway;
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

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
public class ProcessWithInvestigationDeviationIT extends AbstractCamundaAppTest {

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
    void test_investigation_001_createProcessForSanityChecksFailToPass() throws JsonProcessingException, ClassNotFoundException {

        var caseId = "1112";
        var scenarioName = "test_investigation_001_createProcessForSanityChecksFailToPass";

        //Setup mocks
        mockApiGatewayToken();
        var scenarioAfterActualization= mockActualization(caseId, scenarioName);

        // Mock deviation
        var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, scenarioAfterActualization);
        var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
        var scenarioAfterSanityCheckFail = mockCaseDataGet(caseId, scenarioName, scenarioAfterUpdateStatus,
                "investigation_sanity-checks--fail-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "caseTypeParameter", "ANMALAN_ATTEFALL",
                        "statusTypeParameter", "Ärende inkommit",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));
        var scenarioAfterSanityChecks = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterSanityCheckFail);
        var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks);
        var scenarioAfterConstructDecision = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules);
        mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterConstructDecision);

        // Normal mocks
        mockDecision(caseId, scenarioName);
        final var stateAfterExcecution = mockExecution(caseId, scenarioName);
        mockFollowUp(caseId, scenarioName, stateAfterExcecution);

        // Start process
        final var startResponse = setupCall()
                .withServicePath("/2281/process/start/1112")
                .withHttpMethod(POST)
                .withExpectedResponseStatus(ACCEPTED)
                .sendRequest()
                .andReturnBody(StartProcessResponse.class);

        // Wait for process to be waiting for update of errand
        awaitProcessState("investigation_sanity_check_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
                // Investigation with deviation
                .with(tuple("Investigation", "investigation_phase"))
                .with(tuple("Start investigation phase", "start_investigation_phase"))
                .with(tuple("Update phase", "external_task_investigation_update_phase"))
                .with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
                // Sanity check failed
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Wait for update", "investigation_sanity_check_is_update_available"))
                // Sanity check passed
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                .with(tuple("End investigation phase", "end_investigation_phase"))
                .with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
                .with(decisionPathway())
                .with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
                .with(handlingPathway())
                .with(executionPathway())
                .with(followUpPathway())
                .with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
                .with(tuple("End process", "end_process")));
    }

    @Test
    void test_investigation_002_createProcessForPhaseActionNotComplete() throws JsonProcessingException, ClassNotFoundException {

        var caseId = "1213";
        var scenarioName = "test_investigation_002_createProcessForPhaseActionNotComplete";

        //Setup mocks
        mockApiGatewayToken();
        var scenarioAfterActualization = mockActualization(caseId, scenarioName);
        // Mock deviation
        var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, scenarioAfterActualization);
        var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
        var scenarioAfterSanityChecks = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterUpdateStatus, "willNotComplete");
        var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks, "willNotComplete", true);
        var scenarioAfterConstructDecision = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules, "willNotComplete");
        var scenarioAfterCheckPhaseNotCompletedGet = mockCaseDataGet(caseId, scenarioName, scenarioAfterConstructDecision,
                "investigation_check-phase-action_task-worker---api-casedata-get-errand-willNotComplete",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));
        var scenarioAfterCheckPhaseNotCompletedPatch = mockCaseDataPatch(caseId, scenarioName, scenarioAfterCheckPhaseNotCompletedGet,
                "investigation_check-phase-action_task-worker---api-casedata-patch-errand-willNotComplete",
                equalToJson(createPatchBody("Utredning", "UNKNOWN", "WAITING", "Utredning")));

        var scenarioAfterSanityChecks2 = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterCheckPhaseNotCompletedPatch);
        var scenarioAfterExecuteRules2 = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks2);
        var scenarioAfterConstructDecision2 = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules2);
        mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterConstructDecision2);
        // Normal mocks
        mockDecision(caseId, scenarioName);
        final var stateAfterExcecution = mockExecution(caseId, scenarioName);
        mockFollowUp(caseId, scenarioName, stateAfterExcecution);


        // Start process
        final var startResponse = setupCall()
                .withServicePath("/2281/process/start/1213")
                .withHttpMethod(POST)
                .withExpectedResponseStatus(ACCEPTED)
                .sendRequest()
                .andReturnBody(StartProcessResponse.class);

        // Wait for process to be waiting for update of errand
        awaitProcessState("investigation_phase_action_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
                // Investigation with deviation
                .with(tuple("Investigation", "investigation_phase"))
                .with(tuple("Start investigation phase", "start_investigation_phase"))
                .with(tuple("Update phase", "external_task_investigation_update_phase"))
                .with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                // Phase action not complete
                .with(tuple("Wait for update", "investigation_phase_action_is_update_available"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                .with(tuple("End investigation phase", "end_investigation_phase"))
                .with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
                .with(decisionPathway())
                .with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
                .with(handlingPathway())
                .with(executionPathway())
                .with(followUpPathway())
                .with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
                .with(tuple("End process", "end_process")));
    }

    @Test
    void test_investigation_003_createProcessForCancelInInvestigation() throws JsonProcessingException, ClassNotFoundException {

        var caseId = "1314";
        var scenarioName = "test_investigation_003_createProcessForCancelInInvestigation";

        //Setup mocks
        mockApiGatewayToken();
        var scenarioAfterActualization = mockActualization(caseId, scenarioName);

        // Mock deviation
        var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, scenarioAfterActualization);
        var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
        var scenarioAfterSanityChecks = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterUpdateStatus);
        var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks);
        var scenarioAfterConstructDecision = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules);

        var scenarioAfterCheckPhaseCancel = mockCaseDataGet(caseId, scenarioName, scenarioAfterConstructDecision,
                "investigation_check-phase-action_task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "CANCEL",
                        "displayPhaseParameter", "Utredning"));

        mockCaseDataPatch(caseId, scenarioName, scenarioAfterCheckPhaseCancel,
                "investigation_check-phase-action_task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Utredning", "CANCEL", "CANCELED", "Utredning")));

        // Start process
        final var startResponse = setupCall()
                .withServicePath("/2281/process/start/1314")
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
                .with(actualizationPathway())
                .with(tuple("Gateway isCitizen", "gateway_is_citizen"))
                // Investigation with deviation
                .with(tuple("Investigation", "investigation_phase"))
                .with(tuple("Start investigation phase", "start_investigation_phase"))
                .with(tuple("Update phase", "external_task_investigation_update_phase"))
                .with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                // Phase action canceled
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                .with(tuple("End when canceled", "end_investigation_canceled"))
                .with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
                .with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
                .with(tuple("End process", "end_process")));
    }

    @Test
    void test_investigation_004_createProcessValidationErrorInBRToComplete() throws JsonProcessingException, ClassNotFoundException {

        var caseId = "1617";
        var scenarioName = "test_investigation_004_createProcessValidationErrorInBRToComplete";

        //Setup mocks
        mockApiGatewayToken();
        var scenarioAfterActualization = mockActualization(caseId, scenarioName);
        // Mock deviation
        var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, scenarioAfterActualization);
        var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
        var scenarioAfterSanityChecks = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterUpdateStatus, "willNotComplete");
        // Returns validation error
        var scenarioAfterExecuteRules = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks, "willNotComplete", false);
        var scenarioAfterConstructDecisionGet = mockCaseDataGet(caseId, scenarioName, scenarioAfterExecuteRules,
                "construct-recommended-decision-task-worker-rejection---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));
        var scenarioAfterConstructDecisionPatch = mockCaseDataDecisionPatch(caseId, scenarioName, scenarioAfterConstructDecisionGet,
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
        //Will loop back and wait for update
        var scenarioAfterCheckPhaseNotCompletedGet = mockCaseDataGet(caseId, scenarioName, scenarioAfterConstructDecisionPatch,
                "investigation_check-phase-action_task-worker---api-casedata-get-errand-willNotComplete",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Utredning",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Utredning"));
        var scenarioAfterCheckPhaseNotCompletedPatch = mockCaseDataPatch(caseId, scenarioName, scenarioAfterCheckPhaseNotCompletedGet,
                "investigation_check-phase-action_task-worker---api-casedata-patch-errand-willNotComplete",
                equalToJson(createPatchBody("Utredning", "UNKNOWN", "WAITING", "Utredning")));

        // Passes on second attempt
        var scenarioAfterSanityChecks2 = mockInvestigationSanityChecks(caseId, scenarioName, scenarioAfterCheckPhaseNotCompletedPatch);
        var scenarioAfterExecuteRules2 = mockInvestigationExecuteRules(caseId, scenarioName, scenarioAfterSanityChecks2);
        var scenarioAfterConstructDecision2 = mockInvestigationConstructDecision(caseId, scenarioName, scenarioAfterExecuteRules2);
        mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterConstructDecision2);
        // Normal mocks
        mockDecision(caseId, scenarioName);
        mockExecution(caseId, scenarioName);
        mockFollowUp(caseId, scenarioName);

        // Start process
        final var startResponse = setupCall()
                .withServicePath("/2281/process/start/1617")
                .withHttpMethod(POST)
                .withExpectedResponseStatus(ACCEPTED)
                .sendRequest()
                .andReturnBody(StartProcessResponse.class);

        // Wait for process to be waiting for update of errand
        awaitProcessState("investigation_phase_action_is_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

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
                // Investigation with deviation
                .with(tuple("Investigation", "investigation_phase"))
                .with(tuple("Start investigation phase", "start_investigation_phase"))
                .with(tuple("Update phase", "external_task_investigation_update_phase"))
                .with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                // Phase action not complete
                .with(tuple("Wait for update", "investigation_phase_action_is_update_available"))
                .with(tuple("Sanity checks", "external_task_investigation_sanity_check"))
                .with(tuple("Sanity check passed", "gateway_investigation_sanity_check"))
                .with(tuple("Execute rules", "external_task_investigation_execute_rules"))
                .with(tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"))
                .with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
                .with(tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"))
                .with(tuple("End investigation phase", "end_investigation_phase"))
                .with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
                .with(decisionPathway())
                .with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
                .with(handlingPathway())
                .with(executionPathway())
                .with(followUpPathway())
                .with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
                .with(tuple("End process", "end_process")));
    }
}
