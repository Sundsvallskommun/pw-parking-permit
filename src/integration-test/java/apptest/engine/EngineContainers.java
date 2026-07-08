package apptest.engine;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * JVM-wide singleton engine containers, shared across all integration tests so each engine image starts at most once
 * per test run. Two separate containers let the Camunda and Operaton test suites exercise their respective engines.
 * The containers are started lazily on first use (see {@link #baseUrl}) and reaped by Testcontainers' Ryuk on JVM exit
 * - there is intentionally no per-class teardown so the same engine is reused by every scenario.
 */
public final class EngineContainers {

	private static final String CAMUNDA_IMAGE = "camunda/camunda-bpm-platform:run-7.24.0";
	private static final String OPERATON_IMAGE = "operaton/operaton:latest";

	public static final GenericContainer<?> CAMUNDA = create(CAMUNDA_IMAGE);
	public static final GenericContainer<?> OPERATON = create(OPERATON_IMAGE);

	private EngineContainers() {}

	@SuppressWarnings("resource")
	private static GenericContainer<?> create(String image) {
		// "/" answers 302 on both distributions, so wait on the REST engine endpoint which answers 200 once the engine is up.
		return new GenericContainer<>(image)
			.waitingFor(Wait.forHttp("/engine-rest/engine").forStatusCode(200))
			.withExposedPorts(8080);
	}

	/**
	 * Starts the given engine container if it is not already running and returns its {@code /engine-rest} base URL.
	 */
	public static String baseUrl(GenericContainer<?> container) {
		if (!container.isRunning()) {
			container.start();
		}
		return "http://localhost:" + container.getMappedPort(8080) + "/engine-rest";
	}
}
