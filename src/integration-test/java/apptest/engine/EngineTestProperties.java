package apptest.engine;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Points the application's engine-facing properties at the live Operaton container for an integration test. Test base
 * classes delegate their {@code @DynamicPropertySource} to {@link #registerOperaton}.
 * <p>
 * All three URLs target the same container: the external task client poll URL ({@code camunda.bpm.client.base-url}),
 * the Camunda Feign client ({@code integration.camunda.url}, which the test helpers also use to read process history),
 * and the Operaton Feign client ({@code integration.operaton.url}). {@code process-engine.type=operaton} makes the
 * workers' write-path target the Operaton REST API. Since Operaton is API-compatible with Camunda 7, the Camunda read
 * client works against it.
 * <p>
 * Note that setting {@code camunda.bpm.client.base-url} explicitly overrides the derivation from
 * {@code process-engine.type} that application.yaml performs, so these tests do not cover it - see
 * {@code ExternalTaskClientBaseUrlDerivationTest}.
 */
public final class EngineTestProperties {

	private EngineTestProperties() {}

	public static void registerOperaton(DynamicPropertyRegistry registry) {
		final var engineBaseUrl = EngineContainers.operatonBaseUrl();
		registry.add("integration.camunda.url", () -> engineBaseUrl);
		registry.add("camunda.bpm.client.base-url", () -> engineBaseUrl);
		registry.add("integration.operaton.url", () -> engineBaseUrl);
		registry.add("process-engine.type", () -> "operaton");
	}
}
