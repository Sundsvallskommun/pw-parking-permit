package se.sundsvall.parkingpermit.integration.operaton.configuration;

import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import static java.util.Objects.nonNull;
import static se.sundsvall.parkingpermit.integration.operaton.configuration.OperatonConfiguration.CLIENT_ID;

/**
 * Adds a WSO2 client-credentials bearer token to every external task request (fetchAndLock/complete/handleFailure) so
 * the Operaton instance can poll api-service-operaton, which sits behind the OAuth2-secured gateway. Only registered
 * when this instance targets Operaton (see {@link OperatonExternalTaskClientConfiguration}); the Camunda instance polls
 * its engine directly without a token.
 */
class OperatonExternalTaskAuthInterceptor implements ClientRequestInterceptor {

	private static final String PRINCIPAL = "operaton-external-task-client";

	private final OAuth2AuthorizedClientManager authorizedClientManager;

	OperatonExternalTaskAuthInterceptor(final OAuth2AuthorizedClientManager authorizedClientManager) {
		this.authorizedClientManager = authorizedClientManager;
	}

	@Override
	public void intercept(final ClientRequestContext requestContext) {
		final var authorizedClient = authorizedClientManager.authorize(
			OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_ID).principal(PRINCIPAL).build());

		if (nonNull(authorizedClient)) {
			requestContext.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue());
		}
	}
}
