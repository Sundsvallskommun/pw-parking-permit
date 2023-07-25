package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;

import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
@Disabled("due to modifications in actualization")
class CreateProcessIT extends AbstractCamundaAppTest {

	@Autowired
	private CamundaClient camundaClient;

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
			.withServicePath("/process/start/0") // Even number indicates a citizen
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		await()
			.ignoreExceptions()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// Verify process pathways in schema
		final var historicalActivities = camundaClient.getHistoricActivities(startResponse.getProcessId());

		// Activity actualization has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_actualization"::equals).count()).isOne();
		// Activity investigation has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_investigation"::equals).count()).isOne();
		// Activity decision has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_decision"::equals).count()).isOne();
		// Activity handling has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_handling"::equals).count()).isOne();
		// Activity execution has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_execution"::equals).count()).isOne();
		// Activity follow up has been executed 1 time
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("call_activity_follow_up"::equals).count()).isOne();

		// External tasks for automatic denial has not been executed
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_update_errand_phase"::equals).count()).isZero();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_add_denial_decision"::equals).count()).isZero();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_update_errand_status"::equals).count()).isZero();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_send_denial_decision"::equals).count()).isZero();
		assertThat(historicalActivities.stream().map(HistoricActivityInstanceDto::getActivityId).filter("external_task_add_message"::equals).count()).isZero();
	}

	@Test
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
