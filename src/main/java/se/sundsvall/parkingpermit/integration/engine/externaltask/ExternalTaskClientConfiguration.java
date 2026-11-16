package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalTaskClientConfiguration {

	// The client registers every interceptor bean, so a Camunda instance must not have this one
	@Bean
	@ConditionalOnProperty(name = "process-engine.type", havingValue = "operaton")
	ClientRequestInterceptor operatonExternalTaskAuthInterceptor(final OperatonExternalTaskToken token) {
		return new OperatonExternalTaskAuthInterceptor(token);
	}
}
