package apptest;

import com.fasterxml.jackson.core.JsonProcessingException;
import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;

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
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
class CreateProcessIT extends AbstractCamundaAppTest {

	private static final int DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS = 30;

	@BeforeEach
	void setup() {
		setDefaultPollInterval(500, MILLISECONDS);
		setDefaultPollDelay(ZERO);
		setDefaultTimeout(Duration.ofSeconds(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS));

		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getDeployments(null, "%.bpmn", null).size(), equalTo(7));
	}

	@Test
	void test001_createProcessForCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/process/start/123")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse(APPLICATION_JSON, false)
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify wiremock stubs
		verifyStubsAndResetWiremock();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				//Actualization
				tuple("Actualization", "call_activity_actualization"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("End actualization phase", "end_actualization_phase"),
				//GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				//Investigation
				tuple("Investigation", "call_activity_investigation"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("End investigation phase", "end_investigation_phase"),
				//Decision
				tuple("Decision", "call_activity_decision"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase?", "check_update_phase"),
				tuple("Update phase on errand", "external_task_update_phase_task"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("End decision phase", "end_decision_phase"),
				//Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("Dummy Task", "external_task_handling_dummy_task"),
				tuple("End handling phase", "end_handling_phase"),
				//Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Dummy Task", "external_task_execution_dummy_task"),
				tuple("End execution phase", "end_execution_phase"),
				//Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Dummy Task", "external_task_follow_up_dummy_task"),
				tuple("End follow up phase", "end_follow_up_phase"),
				//GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}

	@Test
	void test002_createProcessForNonCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/process/start/456")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse(APPLICATION_JSON, false)
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify stubs and reset wiremock
		verifyStubsAndResetWiremock();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				//Actualization
				tuple("Actualization", "call_activity_actualization"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("End actualization phase", "end_actualization_phase"),
				//Denial
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

				tuple("End process", "end_process"));
	}

	@Test
	void test003_createProcessForDecisionNotFinalToFinal() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/process/start/789")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse(APPLICATION_JSON, false)
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> eventSubscription.getActivityId().equals("is_case_update_available")).count(), equalTo(1L));

		// Update process
		setupCall()
			.withServicePath("/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse(APPLICATION_JSON, false);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify stubs and reset wiremock
		verifyStubsAndResetWiremock();

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				//Actualization
				tuple("Actualization", "call_activity_actualization"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("End actualization phase", "end_actualization_phase"),
				//GW isCitizen
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				//Investigation
				tuple("Investigation", "call_activity_investigation"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Update phase", "external_task_investigation_update_phase"),
				tuple("Sanity checks", "external_task_investigation_sanity_check"),
				tuple("Sanity check passed", "gateway_investigation_sanity_check"),
				tuple("Execute rules", "external_task_investigation_execute_rules"),
				tuple("Construct recommended decision and update case", "external_task_investigation_construct_decision"),
				tuple("End investigation phase", "end_investigation_phase"),
				//Decision
				tuple("Decision", "call_activity_decision"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase?", "check_update_phase"),
				tuple("Update phase on errand", "external_task_update_phase_task"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("End decision phase", "end_decision_phase"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				tuple("Is caseUpdateAvailiable", "is_case_update_available"),
				//Decision not final
				tuple("Decision", "call_activity_decision"),
				tuple("Start decision phase", "start_decision_phase"),
				tuple("Update phase?", "check_update_phase"),
				tuple("Check if decision is made", "external_task_check_decision_task"),
				tuple("End decision phase", "end_decision_phase"),
				tuple("Gateway is decision final", "gateway_is_decision_final"),
				//Handling
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("Dummy Task", "external_task_handling_dummy_task"),
				tuple("End handling phase", "end_handling_phase"),
				//Execution
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Dummy Task", "external_task_execution_dummy_task"),
				tuple("End execution phase", "end_execution_phase"),
				//Follow up
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Dummy Task", "external_task_follow_up_dummy_task"),
				tuple("End follow up phase", "end_follow_up_phase"),
				//GW isCitizen
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),

				tuple("End process", "end_process"));
	}
}
