package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ExternalTaskClientConfiguration {

	// Wired for both engine types, since a Camunda engine polled directly never answers 401
	@Bean
	@Primary
	BackoffStrategy backoffStrategy(final BackoffProperties properties, final OperatonExternalTaskToken token) {
		return new TokenEvictingBackoffStrategy(properties.initTime(), properties.factor(), properties.maxTime(), token);
	}

	// The client registers every interceptor bean, so a Camunda instance must not have this one
	@Bean
	@ConditionalOnProperty(name = "process-engine.type", havingValue = "operaton")
	ClientRequestInterceptor operatonExternalTaskAuthInterceptor(final OperatonExternalTaskToken token) {
		return new OperatonExternalTaskAuthInterceptor(token);
	}
}
