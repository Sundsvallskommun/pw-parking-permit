package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.Collections.reverseOrder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

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

	private final Logger logger;

	AbstractCamundaAppTest() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

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

	protected void awaitProcessState(String state, long timeoutInSeconds) {
		await()
			.ignoreExceptions()
			.atMost(timeoutInSeconds, SECONDS)
			.failFast("Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> state.equals(eventSubscription.getActivityId())).count(), equalTo(1L));
	}

	protected void assertProcessPathway(String processId, boolean acceptDuplication, ArrayList<Tuple> list) {
		final var element = assertThat(getProcessInstanceRoute(processId))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrderElementsOf(list);
		if (!acceptDuplication) {
			element.doesNotHaveDuplicates();
		}
	}

	void logMockInformation() {
		final var fixedColumnWidthFormat = "%-100s"; // Fixed 100 char long colum width

		wiremock.getAllScenarios().getScenarios().stream().forEach(scenario -> {
			logger.info("Scenario:" + scenario.getName());

			logger.info(String.format(fixedColumnWidthFormat, "[From state]") + String.format(fixedColumnWidthFormat, "[To state]") + "[Url match]");

			wiremock.getStubMappings().stream()
				.sorted(reverseOrder((stub1, stub2) -> Long.compare(stub2.getInsertionIndex(), stub1.getInsertionIndex()))) // Reverse to get start of flow at top
				.forEach(mapping -> logger.info(String.format(fixedColumnWidthFormat, mapping.getRequiredScenarioState()) +
					String.format(fixedColumnWidthFormat, mapping.getNewScenarioState()) +
					mapping.getRequest().getMethod() + " " + mapping.getRequest().getUrl()));
		});
	}
}
