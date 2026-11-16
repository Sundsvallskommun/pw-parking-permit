package se.sundsvall.parkingpermit.integration.engine.externaltask;

import feign.RequestTemplate;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.client.exception.ConnectionLostException;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

class OperatonExternalTaskTokenTest {

	private static final ClientRegistration REGISTRATION = ClientRegistration.withRegistrationId("operaton")
		.clientId("the-client-id")
		.clientSecret("the-client-secret")
		.authorizationGrantType(CLIENT_CREDENTIALS)
		.tokenUri("http://localhost/token")
		.scope("openid")
		.build();

	private final List<OAuth2ClientCredentialsGrantRequest> tokenRequests = new ArrayList<>();
	private final List<Instant> issuedExpiresAt = new ArrayList<>();
	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

	private OperatonExternalTaskToken token;

	@BeforeEach
	void setUp() {
		token = createToken(tokenRequests, issuedExpiresAt, clock);
	}

	@Test
	void tokenRequestCarriesRegistrationScopesAndOneDeviceScope() {
		token.getAuthorizationHeader();

		assertThat(tokenRequests).hasSize(1);
		assertThat(tokenRequests.getFirst().getClientRegistration().getRegistrationId()).isEqualTo("operaton");
		assertThat(tokenRequests.getFirst().getClientRegistration().getScopes())
			.hasSize(2)
			.contains("openid")
			.filteredOn(scope -> scope.startsWith("device_"))
			.singleElement()
			.satisfies(scope -> assertThat(scope).matches("device_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
	}

	@Test
	void twoInstancesRequestDifferentDeviceScopes() {
		final var otherTokenRequests = new ArrayList<OAuth2ClientCredentialsGrantRequest>();
		final var otherToken = createToken(otherTokenRequests, new ArrayList<>(), clock);

		token.getAuthorizationHeader();
		otherToken.getAuthorizationHeader();

		assertThat(deviceScope(tokenRequests.getFirst())).isNotEqualTo(deviceScope(otherTokenRequests.getFirst()));
	}

	@Test
	void exposedDeviceScopeIsTheOneSentInTheTokenRequest() {
		token.getAuthorizationHeader();

		assertThat(token.getDeviceScope())
			.startsWith("device_")
			.isEqualTo(deviceScope(tokenRequests.getFirst()));
	}

	@Test
	void validCachedTokenIsReusedWithoutNewTokenRequest() {
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		clock.set(issuedExpiresAt.getFirst().minusSeconds(61));

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		assertThat(tokenRequests).hasSize(1);
	}

	@Test
	void tokenWithinSixtySecondsOfExpiryIsRenewed() {
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		clock.set(issuedExpiresAt.getFirst().minusSeconds(59));

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-2");
		assertThat(tokenRequests).hasSize(2);
	}

	@Test
	void evictedTokenIsReplacedOnNextFetch() {
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");

		token.evict();

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-2");
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-2");
		assertThat(tokenRequests).hasSize(2);
	}

	@Test
	void evictBeforeAnyTokenIsNoop() {
		token.evict();

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		assertThat(tokenRequests).hasSize(1);
	}

	@Test
	void evictKeepsTokenFetchedAfterTheOneLastHandedOut() {
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		// Another token replaces the cached one behind the back of the token instance
		final var oAuth2RequestInterceptor = (OAuth2RequestInterceptor) ReflectionTestUtils.getField(token, "oAuth2RequestInterceptor");
		oAuth2RequestInterceptor.removeToken("Bearer token-1");
		final var template = new RequestTemplate();
		oAuth2RequestInterceptor.apply(template);
		assertThat(template.headers().get("Authorization")).containsExactly("Bearer token-2");

		token.evict();

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-2");
		assertThat(tokenRequests).hasSize(2);
	}

	@Test
	void status401IsUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(restException(401))).isTrue();
	}

	@Test
	void status401WrappedByClientIsUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new UnknownHttpErrorException("Unauthorized", restException(401)))).isTrue();
	}

	@Test
	void status403IsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(restException(403))).isFalse();
	}

	@Test
	void status500IsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new EngineException("Internal error", restException(500)))).isFalse();
	}

	@Test
	void restExceptionWithoutStatusIsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new RestException("No status", "type", 0))).isFalse();
	}

	@Test
	void connectionLostIsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new ConnectionLostException("Connection lost", new IOException()))).isFalse();
	}

	@Test
	void exceptionNotFromRestCallIsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new IllegalStateException("401"))).isFalse();
	}

	@Test
	void nullIsNotUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(null)).isFalse();
	}

	private static OperatonExternalTaskToken createToken(final List<OAuth2ClientCredentialsGrantRequest> requests, final List<Instant> expiresAt, final Clock clock) {
		final var newToken = new OperatonExternalTaskToken(new InMemoryClientRegistrationRepository(REGISTRATION));

		// Replaces only the token endpoint call; expiry handling is the real client credentials provider's
		final var provider = OAuth2AuthorizedClientProviderBuilder.builder()
			.clientCredentials(clientCredentials -> clientCredentials
				.clock(clock)
				.accessTokenResponseClient(grantRequest -> {
					requests.add(grantRequest);
					final var response = OAuth2AccessTokenResponse.withToken("token-" + requests.size())
						.tokenType(OAuth2AccessToken.TokenType.BEARER)
						.expiresIn(3600)
						.build();
					expiresAt.add(response.getAccessToken().getExpiresAt());
					return response;
				}))
			.build();
		final var oAuth2RequestInterceptor = ReflectionTestUtils.getField(newToken, "oAuth2RequestInterceptor");
		final var manager = (AuthorizedClientServiceOAuth2AuthorizedClientManager) ReflectionTestUtils.getField(oAuth2RequestInterceptor, "authorizedClientManager");
		manager.setAuthorizedClientProvider(provider);
		return newToken;
	}

	private static String deviceScope(final OAuth2ClientCredentialsGrantRequest request) {
		return request.getClientRegistration().getScopes().stream().filter(scope -> scope.startsWith("device_")).findFirst().orElseThrow();
	}

	private static RestException restException(final int httpStatusCode) {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(httpStatusCode);
		return restException;
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(final Instant instant) {
			this.instant = instant;
		}

		void set(final Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(final ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
