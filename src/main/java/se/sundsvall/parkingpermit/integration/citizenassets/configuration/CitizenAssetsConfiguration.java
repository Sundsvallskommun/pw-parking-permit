package se.sundsvall.parkingpermit.integration.citizenassets.configuration;

import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
public class CitizenAssetsConfiguration {

	public static final String CLIENT_ID = "citizenassets";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(ClientRegistrationRepository clientRepository, CitizenAssetsProperties citizenAssetsProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestTimeoutsInSeconds(citizenAssetsProperties.connectTimeout(), citizenAssetsProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
