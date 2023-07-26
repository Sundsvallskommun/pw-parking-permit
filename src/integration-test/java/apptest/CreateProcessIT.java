package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;

import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
class CreateProcessIT extends AbstractCamundaAppTest {

	@BeforeEach
	void setup() {
		setDefaultPollInterval(500, MILLISECONDS);
		setDefaultPollDelay(Duration.ZERO);
		setDefaultTimeout(Duration.ofSeconds(30));

		await()
			.ignoreExceptions()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getDeployments(null, "%.bpmn", null).size(), equalTo(7));
	}

	@Test
	void test001_createProcessForCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/process/start/123")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify process pathway.
		assertThat(getProcessInstanceRoute(startResponse.getProcessId()))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrder(
				tuple("Start process", "start_process"),
				tuple("Actualization", "call_activity_actualization"),
				tuple("Start actualization phase", "start_actualization_phase"),
				tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"),
				tuple("End actualization phase", "end_actualization_phase"),
				tuple("Gateway isCitizen", "gateway_is_citizen"),
				tuple("Investigation", "call_activity_investigation"),
				tuple("Start investigation phase", "start_investigation_phase"),
				tuple("Dummy Task", "external_task_investigation_dummy_task"),
				tuple("End investigation phase", "end_investigation_phase"),
				tuple("Decision", "call_activity_decision"),
				tuple("Start decision phase", "Event_17p8i8h"),
				tuple("Dummy Task", "external_task_decsion_dummy_task"),
				tuple("End decision phase", "end_decision_phase"),
				tuple("Handling", "call_activity_handling"),
				tuple("Start handling phase", "start_handling_phase"),
				tuple("Dummy Task", "external_task_handling_dummy_task"),
				tuple("End handling phase", "end_handling_phase"),
				tuple("Execution", "call_activity_execution"),
				tuple("Start execution phase", "start_execution_phase"),
				tuple("Dummy Task", "external_task_execution_dummy_task"),
				tuple("End execution phase", "end_execution_phase"),
				tuple("Follow up", "call_activity_follow_up"),
				tuple("Start follow up phase", "start_follow_up_phase"),
				tuple("Dummy Task", "external_task_follow_up_dummy_task"),
				tuple("End follow up phase", "end_follow_up_phase"),
				tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"),
				tuple("End process", "end_process"));
	}

	@Test
	@Disabled("due to modifications in actualization")
	void test002_createProcessForNonCitizen() throws JsonProcessingException, ClassNotFoundException {

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/process/start/1") // Odd number indicates a non citizen
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse(MediaType.APPLICATION_JSON, false)
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify stubs and reset wiremock
		verifyStubsAndResetWiremock();

		// Verify process pathways in schema
		final var historicalActivities = camundaClient.getHistoricActivities(startResponse.getProcessId());

		// Activity actualization has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_actualization"::equals).count()).isOne();
		// External tasks for automatic denial has been executed 1 time each
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_update_errand_phase"::equals).count()).isOne();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_add_denial_decision"::equals).count()).isOne();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_update_errand_status"::equals).count()).isOne();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_send_denial_decision"::equals).count()).isOne();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_add_message"::equals).count()).isOne();

		// Activity investigation has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_investigation"::equals).count()).isZero();
		// Activity decision has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_decision"::equals).count()).isZero();
		// Activity handling has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_handling"::equals).count()).isZero();
		// Activity execution has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_execution"::equals).count()).isZero();
		// Activity follow up has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_follow_up"::equals).count()).isZero();
	}
}
