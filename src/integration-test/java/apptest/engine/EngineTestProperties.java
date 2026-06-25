package apptest.engine;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Points the application's engine-facing properties at a live engine container for an integration test. Each test leaf
 * picks an engine by delegating its {@code @DynamicPropertySource} to one of these methods.
 * <p>
 * All three URLs target the same container: the external task client poll URL ({@code camunda.bpm.client.base-url}),
 * the Camunda Feign client ({@code integration.camunda.url}, which the test helpers also use to read process history),
 * and the Operaton Feign client ({@code integration.operaton.url}). {@code process-engine.type} selects which engine the
 * workers' write-path targets. Since Operaton is API-compatible with Camunda 7, the read helpers work against either.
 */
public final class EngineTestProperties {

	private EngineTestProperties() {}

	public static void registerCamunda(DynamicPropertyRegistry registry) {
		register(registry, EngineContainers.baseUrl(EngineContainers.CAMUNDA), "camunda");
	}

	public static void registerOperaton(DynamicPropertyRegistry registry) {
		register(registry, EngineContainers.baseUrl(EngineContainers.OPERATON), "operaton");
	}

	private static void register(DynamicPropertyRegistry registry, String engineBaseUrl, String engineType) {
		registry.add("integration.camunda.url", () -> engineBaseUrl);
		registry.add("camunda.bpm.client.base-url", () -> engineBaseUrl);
		registry.add("integration.operaton.url", () -> engineBaseUrl);
		registry.add("process-engine.type", () -> engineType);
	}
}
