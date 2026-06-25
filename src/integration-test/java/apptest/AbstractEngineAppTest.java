package apptest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Engine-neutral base for the testcontainer-driven process tests. Holds the shared assertion and await helpers; the
 * per-engine base classes (in the {@code apptest.camunda} and {@code apptest.operaton} packages) pick the engine by
 * delegating their {@code @DynamicPropertySource} to {@link apptest.engine.EngineTestProperties}. The
 * {@code camundaClient} is used purely as a read client for process history and works against either engine since
 * Operaton is API-compatible with Camunda 7.
 *
 * @see Camunda API for more details https://docs.camunda.org/rest/camunda-bpm-platform/7.17/
 */
public abstract class AbstractEngineAppTest extends AbstractAppTest {

	private static final String TENANT_ID_PARKING_PERMIT = "PARKING_PERMIT";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private final Logger logger;

	@Autowired
	protected CamundaClient camundaClient;

	@Value("${integration.camunda.url}")
	private String engineBaseUrl;

	protected AbstractEngineAppTest() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	/**
	 * Deletes every process instance left on the engine after a test, so state cannot leak between the test classes that
	 * share the same engine container. Needed because the container is reused across {@code @DirtiesContext} contexts:
	 * without this, an instance that outlives its own test (e.g. after a failure, or because the slower Operaton engine
	 * has not drained it yet) gets picked up by the next class's external task workers and corrupts that class's WireMock
	 * scenario state. Filtered on the parking-permit tenant so the engine's own example processes are left untouched.
	 */
	@AfterEach
	void purgeProcessInstances() throws Exception {
		final var listRequest = HttpRequest.newBuilder(URI.create(engineBaseUrl + "/process-instance?tenantIdIn=" + TENANT_ID_PARKING_PERMIT)).GET().build();
		final var listResponse = HTTP_CLIENT.send(listRequest, HttpResponse.BodyHandlers.ofString());
		final JsonNode instances = OBJECT_MAPPER.readTree(listResponse.body());
		for (final JsonNode instance : instances) {
			final var deleteRequest = HttpRequest.newBuilder(
				URI.create(engineBaseUrl + "/process-instance/" + instance.get("id").asText() + "?skipCustomListeners=true&skipIoMappings=true&failIfNotExists=false"))
				.DELETE().build();
			HTTP_CLIENT.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
		}
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

	protected void logMockInformation() {
		final var fixedColumnWidthFormat = "%-100s"; // Fixed 100 char long colum width

		wiremock.getAllScenarios().getScenarios().stream().forEach(scenario -> {
			logger.info("Scenario:" + scenario.getName());

			logger.info(String.format(fixedColumnWidthFormat, "[From state]") + String.format(fixedColumnWidthFormat, "[To state]") + "[Url match]");

			wiremock.getStubMappings().stream()
				.sorted(reverseOrder((stub1, stub2) -> Long.compare(stub2.getInsertionIndex(), stub1.getInsertionIndex()))) // Reverse to get start of flow at top
				.forEach(mapping -> logger.info(String.format(fixedColumnWidthFormat, mapping.getRequiredScenarioState()) +
					String.format(fixedColumnWidthFormat, mapping.getNewScenarioState()) +
					mapping.getRequest().getMethod() + " " + mapping.getRequest().getUrl() + " " + mapping.getRequest().getBodyPatterns()));
		});
	}
}
