package apptest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import java.time.Duration;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.ACTIVE;
import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
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
			.until(() -> camundaClient.getDeployments("template-process.bpmn", null, null).size(), equalTo(1));
	}

	@Test
	void test001_createProcessWithoutUpdates() throws JsonProcessingException, ClassNotFoundException {

		// === Start process ===
		final var startResponse = setupCall()
			.withServicePath("/process/start/businessKey")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.andReturnBody(StartProcessResponse.class);

		await()
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));

		// ExternalTask_MyWorker has been executed 1 time
		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_MyWorker", false, true, false)).hasSize(1);
		// ExternalTask_CheckData has been executed 1 time
		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_CheckData", false, true, false)).hasSize(1);
	}

	@Test
	void test002_createProcessWithTwoUpdates() throws JsonProcessingException, ClassNotFoundException {

		// === Start process ===
		final var startResponse = setupCall()
			.withServicePath("/process/start/will_need_two_updates")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to start
		await()
			.ignoreExceptions()
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(ACTIVE));

		// Wait for process to be in state "Update Available?"
		await()
			.until(() -> camundaClient.getProcessActivityInstance(startResponse.getProcessId()).getChildActivityInstances().get(0).getActivityName(), equalTo("Update Available?"));

		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_CheckData", false, true, false)).hasSize(1);

		// === Update process first time ===
		setupCall()
			.withServicePath("/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse();

		// Wait for process to execute worker
		await()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getProcessInstanceVariables(startResponse.getProcessId()).get("updateAvailable").getValue(), equalTo(false));
		// Wait for process to be in state "Update Available?"
		await()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getProcessActivityInstance(startResponse.getProcessId()).getChildActivityInstances().get(0).getActivityName(), equalTo("Update Available?"));

		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_CheckData", false, true, false)).hasSize(2);

		// === Update process second time ===
		setupCall()
			.withServicePath("/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse();

		// Wait for process to complete
		await()
			.atMost(30, SECONDS)
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(COMPLETED));


		// ExternalTask_MyWorker has been executed 1 time
		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_MyWorker", false, true, false)).hasSize(1);
		// ExternalTask_CheckData has been executed 3 time
		assertThat(camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_CheckData", false, true, false)).hasSize(3);

	}

	@Test
	void test003_testMyWorkerThrowsException()  throws JsonProcessingException, ClassNotFoundException {
		// === Start process ===
		final var startResponse = setupCall()
			.withServicePath("/process/start/throw_exception")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.andReturnBody(StartProcessResponse.class);

		// Wait for 4 failure (1 ordinary and 3 retries)
		await()
			.atMost(30, SECONDS)
			.ignoreExceptions()
			.until(() -> camundaClient.getHistoricExternalTaskLog(startResponse.getProcessId(), "ExternalTask_MyWorker", false, false, true).size(), equalTo(4));

		var incidents = camundaClient.getHistoricIncidents(startResponse.getProcessId());
		assertThat(incidents).hasSize(1);
		assertThat(incidents.get(0).getIncidentType()).isEqualTo("failedExternalTask");
		assertThat(incidents.get(0).getActivityId()).isEqualTo("ExternalTask_MyWorker");
	}
}
