package se.sundsvall.parkingpermit.integration.rpa.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import feign.Response;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class RpaRetryResponseVerifierTest {
	@Mock
	private Response mockResponse;

	@Mock
	private RpaProperties rpaProperties;

	@InjectMocks
	private RpaRetryResponseVerifier rpaRetryResponseVerifier;
	private static final String RPA_TOKEN_EXPIRED = "RPA Identity Server token has expired";

	@Test
	@DisplayName("Should return true if we get status 401 from service and token has expired")
	void shouldReturnRetryableException() {
		Map<String, Collection<String>> headers = Map.of("WWW-Authenticate", Set.of("Bearer realm=\"https://rpa.base.url/identity\""));
		when(mockResponse.status()).thenReturn(401);
		when(mockResponse.headers()).thenReturn(headers);
		when(rpaProperties.identityServerUrl()).thenReturn("https://rpa.base.url/identity");

		assertThat(rpaRetryResponseVerifier.shouldReturnRetryableException(mockResponse)).isTrue();
	}

	@Test
	@DisplayName("Should return false if we get status 401 from service but no realm is present")
	void shouldReturnRetryableException_NoBearerRealmPresent() {
		Map<String, Collection<String>> headers = Map.of("WWW-Authenticate", Set.of("Bearer realm=\"https://any.other.realm.se/identity\""));
		when(mockResponse.status()).thenReturn(401);
		when(mockResponse.headers()).thenReturn(headers);
		when(rpaProperties.identityServerUrl()).thenReturn("https://rpa.base.url/identity");

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
