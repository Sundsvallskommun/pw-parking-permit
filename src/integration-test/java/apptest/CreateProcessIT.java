package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
class CreateProcessIT extends AbstractCamundaAppTest {

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
	void test001_createProcessForCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/123")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("End handling phase", "end_handling_phase"),
				// Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Update phase", "external_task_execution_update_phase"),
				tuple("Order card", "external_task_execution_order_card_task"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Create Asset", "external_task_execution_create_asset"),
				tuple("End execution phase", "end_execution_phase"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test002_createProcessForNonCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/456")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("End when not citizen of municipality", "end_actualization_not_citizen"),
				// Denial
				tuple("Start automatic denial phase", "start_automatic_denial_phase"),
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				tuple("Update phase on errand", "external_task_update_errand_phase"),
				tuple("Add decision for denial to errand", "external_task_add_denial_decision"),
				tuple("Update errand status", "external_task_update_errand_status"),
				tuple("Send denial decision to applicant", "external_task_send_denial_decision"),
				tuple("Add message to errand", "external_task_add_message"),
				tuple("End automatic denial phase", "end_automatic_denial_phase"),
				tuple("Automatic denial", "subprocess_automatic_denial"),
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),

				tuple("End process", "end_process"));
	}

	@Test
	void test003_createProcessForDecisionNotFinalToFinal() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/789")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "decision_is_case_update_available".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Update process
		setupCall()
			.withServicePath("/2281/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("Is caseUpdateAvailable", "decision_is_case_update_available"),
				// Decision not final,
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("End handling phase", "end_handling_phase"),
				// Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Update phase", "external_task_execution_update_phase"),
				tuple("Order card", "external_task_execution_order_card_task"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Create Asset", "external_task_execution_create_asset"),
				tuple("End execution phase", "end_execution_phase"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test004_createProcessForCancelInActualization() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/789")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End when canceled", "end_actualization_canceled"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),

				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test005_createProcessForActualizationNotComplete() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1011")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "actualization_is_case_update_available".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Update process
		setupCall()
			.withServicePath("/2281/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				// Actualization not complete
				tuple("Wait for complete action", "actualization_is_case_update_available"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("End handling phase", "end_handling_phase"),
				// Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Update phase", "external_task_execution_update_phase"),
				tuple("Order card", "external_task_execution_order_card_task"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Create Asset", "external_task_execution_create_asset"),
				tuple("End execution phase", "end_execution_phase"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test009_createProcessForCardNotExistsToExists() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1415")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "execution_card_check_is_update_available".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Update process
		setupCall()
			.withServicePath("/2281/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("End handling phase", "end_handling_phase"),
				// Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Update phase", "external_task_execution_update_phase"),
				tuple("Order card", "external_task_execution_order_card_task"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				// Card does not exist
				tuple("Wait for existing card", "execution_card_check_is_update_available"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Create Asset", "external_task_execution_create_asset"),
				tuple("End execution phase", "end_execution_phase"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test010_createProcessForCancelnDecision() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1516")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test011_createProcessValidationErrorInBRToComplete() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1617")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "investigation_phase_action_is_update_available".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Update process
		setupCall()
			.withServicePath("/2281/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				// Phase action not complete
				tuple("Wait for update", "investigation_phase_action_is_update_available"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("End handling phase", "end_handling_phase"),
				// Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Update phase", "external_task_execution_update_phase"),
				tuple("Order card", "external_task_execution_order_card_task"),
				tuple("Check if card exists", "external_task_execution_check_if_card_exists"),
				tuple("Is card manufactured", "gateway_card_exists"),
				tuple("Create Asset", "external_task_execution_create_asset"),
				tuple("End execution phase", "end_execution_phase"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test012_createProcessForDecisionRejected() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/process/start/1718")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				// Actualization
				tuple("Actualization", "actualization_phase"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Update phase", "external_task_actualization_update_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
				tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
				tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
				tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
				tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
				tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"),
				tuple("End actualization phase", "end_actualization_phase"),
				// GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				// Investigation
				tuple("Investigation", "investigation_phase"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Update errand status", "external_task_investigation_update_errand_status"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("Check phase action", "external_task_investigation_check_phase_action_task"),
				tuple("Is phase action complete", "gateway_decision_is_phase_action_complete"),
				tuple("End investigation phase", "end_investigation_phase"),
				// GW isCanceled
				tuple("Is canceled in investigation", "gateway_investigation_canceled"),
				// Decision
				tuple("Decision", "decision_phase"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase on errand", "external_task_decision_update_phase"),
				tuple("Update errand status", "external_task_decision_update_errand_status"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				// GW isCanceled
				tuple("Is canceled in decision or not approved", "gateway_decision_canceled"),
				// Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Update phase", "external_task_follow_up_update_phase"),
				tuple("Clean up notes", "external_task_follow_up_clean_up_notes"),
				tuple("End follow up phase", "end_follow_up_phase"),
				// GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test013_createProcessForCancelInActualizationWhenVerifyingAdministrator() throws JsonProcessingException, ClassNotFoundException {
		// Start process
		final var startResponse = setupCall()
				.withServicePath("/2281/process/start/1920")
				.withHttpMethod(POST)
				.withExpectedResponseStatus(ACCEPTED)
				.sendRequest()
				.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
				.ignoreExceptions()
				.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
				.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
				.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
				.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
				.containsExactlyInAnyOrder(
						tuple("Start process", "start_process"),
						// Actualization
						tuple("Actualization", "actualization_phase"),
						tuple("Start actualization phase", "start_actualization_phase"),
						tuple("Update phase", "external_task_actualization_update_phase"),
						tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
						tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
						tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
						tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
						tuple("End when canceled", "end_actualization_canceled"),
						// GW isCitizen
						tuple("Gateway isCitizen", "gateway_is_citizen"),
						// GW isCitizen
						tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

						tuple("End process", "end_process"));
	}

	@Test
	void test014_createProcessWaitingForStakeholderUpdateInActualization() throws JsonProcessingException, ClassNotFoundException {
		// Start process
		final var startResponse = setupCall()
				.withServicePath("/2281/process/start/2021")
				.withHttpMethod(POST)
				.withExpectedResponseStatus(ACCEPTED)
				.sendRequest()
				.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
				.ignoreExceptions()
				.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
				.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
				.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "actualization_wait_for_stakeholder_update".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Update process
		setupCall()
				.withServicePath("/2281/process/update/" + startResponse.getProcessId())
				.withHttpMethod(POST)
				.withExpectedResponseStatus(ACCEPTED)
				.withExpectedResponseBodyIsNull()
				.sendRequest();

		// Wait for process to be waiting for phase action in actualization
		await()
				.ignoreExceptions()
				.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
				.failFast( "Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
				.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> "actualization_is_case_update_available".equals(eventSubscription.getActivityId())).count(), equalTo(1L));

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
				.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
				.containsExactlyInAnyOrder(
						tuple("Start process", "start_process"),
						// Actualization
						tuple("Start actualization phase", "start_actualization_phase"),
						tuple("Update phase", "external_task_actualization_update_phase"),
						tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
						tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"),
						tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
						tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
						// Stakeholder not assigned
						tuple("Wait for case update", "actualization_wait_for_stakeholder_update"),
						tuple("Verify that administrator stakeholder exists", "external_task_actualization_verify_administrator_stakeholder_exists_task"),
						tuple("Is stakeholder with role ADMINISTRATOR assigned", "gateway_actualization_stakeholder_administrator_is_assigned"),
						tuple("Update displayPhase", "external_task_actualization_update_display_phase"),
						tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"),
						tuple("Check phase action", "external_task_actualization_check_phase_action_task"),
						tuple("Is phase action complete", "gateway_actualization_is_phase_action_complete"));
						// Gets stuck in actualization_is_case_update_available (verified by last await) but is not part
						// of history yet since it has not ended.
	}
}
