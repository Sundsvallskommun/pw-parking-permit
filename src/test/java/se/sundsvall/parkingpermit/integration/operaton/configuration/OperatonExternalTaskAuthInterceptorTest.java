package se.sundsvall.parkingpermit.integration.operaton.configuration;

import java.time.Instant;
import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatonExternalTaskAuthInterceptorTest {

	@Mock
	private OAuth2AuthorizedClientManager authorizedClientManagerMock;

	@Mock
	private ClientRequestContext requestContextMock;

	@Test
	void addsBearerTokenHeader() {
		final var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "the-token", Instant.now(), Instant.now().plusSeconds(60));
		final var authorizedClient = mock(OAuth2AuthorizedClient.class);
		when(authorizedClient.getAccessToken()).thenReturn(accessToken);
		when(authorizedClientManagerMock.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

		new OperatonExternalTaskAuthInterceptor(authorizedClientManagerMock).intercept(requestContextMock);

		verify(requestContextMock).addHeader("Authorization", "Bearer the-token");
	}

	@Test
	void doesNotAddHeaderWhenNotAuthorized() {
		when(authorizedClientManagerMock.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

		new OperatonExternalTaskAuthInterceptor(authorizedClientManagerMock).intercept(requestContextMock);

		verifyNoInteractions(requestContextMock);
	}
}
