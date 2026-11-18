package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatonExternalTaskAuthInterceptorTest {

	@Mock
	private OperatonExternalTaskToken tokenMock;

	@Mock
	private ClientRequestContext requestContextMock;

	@Test
	void addsAuthorizationHeaderFromToken() {
		when(tokenMock.getAuthorizationHeader()).thenReturn("Bearer the-token");

		new OperatonExternalTaskAuthInterceptor(tokenMock).intercept(requestContextMock);

		verify(requestContextMock).addHeader("Authorization", "Bearer the-token");
		verify(tokenMock).getAuthorizationHeader();
		verifyNoMoreInteractions(tokenMock, requestContextMock);
	}

	@Test
	void consecutiveCallsReuseTokenWithoutEvicting() {
		when(tokenMock.getAuthorizationHeader()).thenReturn("Bearer the-token");
		final var interceptor = new OperatonExternalTaskAuthInterceptor(tokenMock);

		interceptor.intercept(requestContextMock);
		interceptor.intercept(requestContextMock);

		verify(requestContextMock, times(2)).addHeader("Authorization", "Bearer the-token");
		verify(tokenMock, times(2)).getAuthorizationHeader();
		verifyNoMoreInteractions(tokenMock, requestContextMock);
	}
}
