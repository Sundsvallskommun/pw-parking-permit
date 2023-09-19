package se.sundsvall.parkingpermit.integration.rpa.configuration;

import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpaRetryResponseVerifierTest {
	@Mock
	private Response mockResponse;
	private RpaRetryResponseVerifier rpaRetryResponseVerifier;
	private static final String RPA_TOKEN_EXPIRED = "RPA Identity Server token has expired";

	@BeforeEach
	void init (){
		rpaRetryResponseVerifier = new RpaRetryResponseVerifier();
	}

	@Test
	@DisplayName("Should return true if we get status 401 from service and token has expired")
	void shouldReturnRetryableException() {
		Map<String, Collection<String>>  headers = Map.of("WWW-Authenticate", Set.of("Bearer realm=\"https://robot.sundsvall.se/identity\""));
		when(mockResponse.status()).thenReturn(401);
		when(mockResponse.headers()).thenReturn(headers);

		assertThat(rpaRetryResponseVerifier.shouldReturnRetryableException(mockResponse)).isTrue();
	}

	@Test
	@DisplayName("Should return false if we get status 401 from service but no realm is present")
	void shouldReturnRetryableException_NoBearerRealmPresent() {
		Map<String, Collection<String>> headers = Map.of("WWW-Authenticate", Set.of("Bearer realm=\"https://any.other.realm.se/identity\""));
		when(mockResponse.status()).thenReturn(401);
		when(mockResponse.headers()).thenReturn(headers);

		assertThat(rpaRetryResponseVerifier.shouldReturnRetryableException(mockResponse)).isFalse();
	}

	@Test
	@DisplayName("Should return false if status code is not 401")
	void shouldReturnRetryableException_Not401Status() {
		when(mockResponse.status()).thenReturn(500);

		assertThat(rpaRetryResponseVerifier.shouldReturnRetryableException(mockResponse)).isFalse();
	}

	@Test
	@DisplayName("Should return a correct message equal to RPA_TOKEN_EXPIRED")
	void getMessage() {
		assertThat(rpaRetryResponseVerifier.getMessage()).isEqualTo(RPA_TOKEN_EXPIRED);
	}

}
