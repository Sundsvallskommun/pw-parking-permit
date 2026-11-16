package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Adds the WSO2 bearer token to every external task request, as the Operaton engine sits behind the gateway.
 */
class OperatonExternalTaskAuthInterceptor implements ClientRequestInterceptor {

	private final OperatonExternalTaskToken token;

	OperatonExternalTaskAuthInterceptor(final OperatonExternalTaskToken token) {
		this.token = token;
	}

	@Override
	public void intercept(final ClientRequestContext requestContext) {
		requestContext.addHeader(AUTHORIZATION, token.getAuthorizationHeader());
	}
}
