package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;

/**
 * Test class using testcontainer to execute the process.
 * There are a lot of resources that can be added to CamundaClient
 * to make good assertions. This test class contains a few examples.
 *
 * @see Camunda API for more details https://docs.camunda.org/rest/camunda-bpm-platform/7.17/
 */
@Testcontainers
abstract class AbstractCamundaAppTest extends AbstractAppTest {

	private static final String CAMUNDA_IMAGE_NAME = "camunda/camunda-bpm-platform:run-7.17.0"; // Corresponds to the actual version used.

	@Autowired
	protected CamundaClient camundaClient;

	@SuppressWarnings("resource")
	@Container
	private static final GenericContainer<?> CAMUNDA = new GenericContainer<>(CAMUNDA_IMAGE_NAME)
		.waitingFor(Wait.forHttp("/"))
		.withExposedPorts(8080);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		CAMUNDA.start();
		final var camundaBaseUrl = "http://" + "localhost:" + CAMUNDA.getMappedPort(8080) + "/engine-rest";
		registry.add("integration.camunda.url", () -> camundaBaseUrl);
		registry.add("camunda.bpm.client.base-url", () -> camundaBaseUrl);
	}

	@AfterAll
	static void teardown() {
		CAMUNDA.stop();
	}

	protected List<HistoricActivityInstanceDto> getProcessInstanceRoute(String processInstanceId) {
		return getRoute(processInstanceId, new ArrayList<>());
	}

	private List<HistoricActivityInstanceDto> getRoute(String processInstanceId, List<HistoricActivityInstanceDto> route) {
		if (isNull(processInstanceId)) {
			return route;
		}
		return camundaClient.getHistoricActivities(processInstanceId).stream()
			.filter(e -> e.getEndTime() != null)
			.sorted(comparing(HistoricActivityInstanceDto::getEndTime))
			.flatMap(activity -> concat(Stream.of(activity), getRoute(activity.getCalledProcessInstanceId(), route).stream()))
			.toList();
	}

	protected void awaitProcessCompleted(String processId, long timeoutInSeconds) {
		await()
				.ignoreExceptions()
				.atMost(timeoutInSeconds, SECONDS)
				.failFast("Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
				.until(() -> camundaClient.getHistoricProcessInstance(processId).getState(), equalTo(COMPLETED));
	}

	protected void assertProcessPathway(String processId, ArrayList<Tuple> list) {
		assertThat(getProcessInstanceRoute(processId))
				.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
				.doesNotHaveDuplicates()
				.containsExactlyInAnyOrderElementsOf(list);
	}
}
