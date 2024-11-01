package se.sundsvall.parkingpermit.integration.rpa.configuration;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;
import se.sundsvall.dept44.configuration.feign.retryer.ActionRetryer;

import java.util.List;

import static java.util.Collections.emptySet;

@Import(FeignConfiguration.class)
public class RpaConfiguration {

	public static final String CLIENT_ID = "rpa";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(ClientRegistrationRepository clientRepository, RpaProperties rpaProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new JsonPathErrorDecoder(CLIENT_ID, List.of(409), new JsonPathErrorDecoder.JsonPathSetup(
				"$['message']", "$['errorCode']"), new RpaRetryResponseVerifier(rpaProperties)))
			.withCustomizer(builder -> {
				var oAuth2RequestInterceptor = new OAuth2RequestInterceptor(clientRepository.findByRegistrationId(CLIENT_ID), emptySet());
				builder.requestInterceptor(oAuth2RequestInterceptor);
				builder.retryer(new ActionRetryer(oAuth2RequestInterceptor::removeToken, 1));
			})
			.withRequestTimeoutsInSeconds(rpaProperties.connectTimeout(), rpaProperties.readTimeout())
			.composeCustomizersToOne();
	}
}
