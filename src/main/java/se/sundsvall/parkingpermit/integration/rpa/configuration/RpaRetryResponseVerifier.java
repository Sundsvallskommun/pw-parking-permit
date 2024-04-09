package se.sundsvall.parkingpermit.integration.rpa.configuration;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.stereotype.Component;

import feign.Response;
import se.sundsvall.dept44.configuration.feign.decoder.RetryResponseVerifier;

@Component
public class RpaRetryResponseVerifier implements RetryResponseVerifier {
	private static final String RPA_TOKEN_EXPIRED = "RPA Identity Server token has expired";

	private static final String RPA_WWW_AUTH_HEADER = "Bearer realm=\"%s\"";

	private final RpaProperties rpaProperties;

	RpaRetryResponseVerifier(RpaProperties rpaProperties) {
		this.rpaProperties = rpaProperties;
	}

	@Override
	public boolean shouldReturnRetryableException(Response response) {
		final var rpaAuthHeader = String.format(RPA_WWW_AUTH_HEADER, rpaProperties.identityServerUrl());

		return (response.status() == UNAUTHORIZED.value()) &&
			ofNullable(response.headers().get(WWW_AUTHENTICATE)).orElse(emptySet()).stream()
				.anyMatch(rpaAuthHeader::equals);
	}

	@Override
	public String getMessage() {
		return RPA_TOKEN_EXPIRED;
	}
}
