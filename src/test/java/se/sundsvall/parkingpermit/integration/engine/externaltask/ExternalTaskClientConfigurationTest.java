package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ExternalTaskClientConfigurationTest {

	private final OperatonExternalTaskToken token = mock(OperatonExternalTaskToken.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ExternalTaskClientConfiguration.class)
		.withBean(OperatonExternalTaskToken.class, () -> token)
		.withBean(BackoffProperties.class, () -> new BackoffProperties(300, 3, 9000));

	@Test
	void interceptorRegisteredWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertThat(context).getBean(ClientRequestInterceptor.class)
				.isInstanceOf(OperatonExternalTaskAuthInterceptor.class)
				.hasFieldOrPropertyWithValue("token", token));
		verifyNoInteractions(token);
	}

	@Test
	void interceptorNotRegisteredWhenEngineIsCamunda() {
		contextRunner.withPropertyValues("process-engine.type=camunda")
			.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
		verifyNoInteractions(token);
	}

	@Test
	void interceptorNotRegisteredByDefault() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
		verifyNoInteractions(token);
	}

	@Test
	void tokenEvictingBackoffStrategyRegisteredWhenEngineIsCamunda() {
		contextRunner.withPropertyValues("process-engine.type=camunda")
			.run(context -> assertBackoffStrategy(context.getBean(BackoffStrategy.class)));
		verifyNoInteractions(token);
	}

	@Test
	void tokenEvictingBackoffStrategyRegisteredWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertBackoffStrategy(context.getBean(BackoffStrategy.class)));
		verifyNoInteractions(token);
	}

	private void assertBackoffStrategy(final BackoffStrategy backoffStrategy) {
		assertThat(backoffStrategy)
			.isInstanceOf(TokenEvictingBackoffStrategy.class)
			.hasFieldOrPropertyWithValue("initTime", 300L)
			.hasFieldOrPropertyWithValue("factor", 3F)
			.hasFieldOrPropertyWithValue("maxTime", 9000L)
			.hasFieldOrPropertyWithValue("token", token);
	}
}
