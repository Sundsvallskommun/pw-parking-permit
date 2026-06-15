package se.sundsvall.parkingpermit.integration.operaton.configuration;

import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OperatonExternalTaskClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(OperatonExternalTaskClientConfiguration.class)
		.withBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class))
		.withBean(OAuth2AuthorizedClientService.class, () -> mock(OAuth2AuthorizedClientService.class));

	@Test
	void interceptorRegisteredWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertThat(context).hasSingleBean(ClientRequestInterceptor.class));
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
