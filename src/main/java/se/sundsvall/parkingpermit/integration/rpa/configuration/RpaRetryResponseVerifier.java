package se.sundsvall.parkingpermit.integration.rpa.configuration;

import feign.Response;
import se.sundsvall.dept44.configuration.feign.decoder.RetryResponseVerifier;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class RpaRetryResponseVerifier implements RetryResponseVerifier {

	private static final String RPA_TOKEN_EXPIRED = "RPA Identity Server token has expired";
	private static final String RPA_WWW_AUTH_HEADER = "Bearer realm=\"https://robot.sundsvall.se/identity\"";

	@Override
	public boolean shouldReturnRetryableException(Response response) {

		return response.status() == SC_UNAUTHORIZED &&
			ofNullable(response.headers().get(WWW_AUTHENTICATE)).orElse(emptySet()).stream()
				.anyMatch(RPA_WWW_AUTH_HEADER::equals);
	}

	@Override
	public String getMessage() {
		return RPA_TOKEN_EXPIRED;
	}
}
