package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExternalTaskClientConfigurationTest {

	private final OperatonExternalTaskToken token = mock(OperatonExternalTaskToken.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ExternalTaskClientConfiguration.class)
		.withBean(OperatonExternalTaskToken.class, () -> token);

	@Test
	void interceptorRegisteredWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertThat(context).getBean(ClientRequestInterceptor.class)
				.isInstanceOf(OperatonExternalTaskAuthInterceptor.class)
				.hasFieldOrPropertyWithValue("token", token));
	}

	@Test
	void interceptorNotRegisteredWhenEngineIsCamunda() {
		contextRunner.withPropertyValues("process-engine.type=camunda")
			.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
	}

	@Test
	void interceptorNotRegisteredByDefault() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
	}
}
