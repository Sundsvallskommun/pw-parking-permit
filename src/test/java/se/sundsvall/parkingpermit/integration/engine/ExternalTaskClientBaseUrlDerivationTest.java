package se.sundsvall.parkingpermit.integration.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The external task client's poll URL is derived from {@code process-engine.type} in application.yaml, so an instance
 * can never poll one engine while its workers write to the other. The integration tests set
 * {@code camunda.bpm.client.base-url} explicitly and therefore bypass the derivation - this is where it is covered.
 */
class ExternalTaskClientBaseUrlDerivationTest {

	private static final String POLL_URL = "camunda.bpm.client.base-url";
	private static final String CAMUNDA_URL = "https://camunda.example/engine-rest";
	private static final String OPERATON_URL = "https://operaton.example/engine-rest";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withInitializer(new ConfigDataApplicationContextInitializer())
		.withPropertyValues(
			"config.camunda.base-url=" + CAMUNDA_URL,
			"config.operaton.base-url=" + OPERATON_URL);

	@Test
	void pollUrlIsOperatonWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("config.process-engine-type=operaton")
			.run(context -> assertThat(context.getEnvironment().getProperty(POLL_URL)).isEqualTo(OPERATON_URL));
	}

	@Test
	void pollUrlIsCamundaWhenEngineIsCamunda() {
		contextRunner.withPropertyValues("config.process-engine-type=camunda")
			.run(context -> assertThat(context.getEnvironment().getProperty(POLL_URL)).isEqualTo(CAMUNDA_URL));
	}

	@Test
	void pollUrlCannotResolveWhenTypeIsNotSet() {
		contextRunner.run(context -> assertThatThrownBy(() -> context.getEnvironment().getProperty(POLL_URL))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("process-engine.type"));
	}
}
