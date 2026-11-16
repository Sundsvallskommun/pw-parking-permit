package se.sundsvall.parkingpermit.integration.engine.externaltask;

import java.io.IOException;
import java.util.Map;
import org.camunda.bpm.client.exception.ConnectionLostException;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class UnauthorizedRetryingExternalTaskServiceTest {

	private static final Map<String, Object> VARIABLES = Map.of("key", "value");
	private static final Map<String, Object> LOCAL_VARIABLES = Map.of("localKey", "localValue");

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private OperatonExternalTaskToken tokenMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Test
	void successfulCompleteIsMadeOnceWithoutEviction() {
		wrap().complete(externalTaskMock, VARIABLES);

		verify(externalTaskServiceMock).complete(externalTaskMock, VARIABLES);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void unauthorizedThenSuccessOnCompleteEvictsTokenAndRetriesExactlyOnce() {
		doThrow(unauthorized()).doNothing().when(externalTaskServiceMock).complete(externalTaskMock, VARIABLES);

		wrap().complete(externalTaskMock, VARIABLES);

		final var inOrder = inOrder(externalTaskServiceMock, tokenMock);
		inOrder.verify(externalTaskServiceMock).complete(externalTaskMock, VARIABLES);
		inOrder.verify(tokenMock).evict();
		inOrder.verify(externalTaskServiceMock).complete(externalTaskMock, VARIABLES);
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void unauthorizedTwiceOnCompleteRethrowsSecondFailure() {
		final var secondFailure = unauthorized();
		doThrow(unauthorized()).doThrow(secondFailure).when(externalTaskServiceMock).complete(externalTaskMock, VARIABLES);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(UnknownHttpErrorException.class)
			.isThrownBy(() -> externalTaskService.complete(externalTaskMock, VARIABLES))
			.isSameAs(secondFailure);

		verify(externalTaskServiceMock, times(2)).complete(externalTaskMock, VARIABLES);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void successfulHandleFailureIsMadeOnceWithoutEviction() {
		wrap().handleFailure("id", "worker", "message", 2, 10L);

		verify(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void unauthorizedThenSuccessOnHandleFailureEvictsTokenAndRetriesExactlyOnce() {
		doThrow(unauthorized()).doNothing().when(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L);

		wrap().handleFailure("id", "worker", "message", 2, 10L);

		final var inOrder = inOrder(externalTaskServiceMock, tokenMock);
		inOrder.verify(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L);
		inOrder.verify(tokenMock).evict();
		inOrder.verify(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L);
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void unauthorizedTwiceOnHandleFailureRethrowsSecondFailure() {
		final var secondFailure = unauthorized();
		doThrow(unauthorized()).doThrow(secondFailure).when(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(UnknownHttpErrorException.class)
			.isThrownBy(() -> externalTaskService.handleFailure("id", "worker", "message", 2, 10L))
			.isSameAs(secondFailure);

		verify(externalTaskServiceMock, times(2)).handleFailure("id", "worker", "message", 2, 10L);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void successfulHandleFailureWithVariablesIsMadeOnceWithoutEviction() {
		wrap().handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);

		verify(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void unauthorizedThenSuccessOnHandleFailureWithVariablesRetriesExactlyOnce() {
		doThrow(unauthorized()).doNothing().when(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);

		wrap().handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);

		verify(externalTaskServiceMock, times(2)).handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void unauthorizedTwiceOnHandleFailureWithVariablesRethrowsSecondFailure() {
		final var secondFailure = unauthorized();
		doThrow(unauthorized()).doThrow(secondFailure).when(externalTaskServiceMock).handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(UnknownHttpErrorException.class)
			.isThrownBy(() -> externalTaskService.handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES))
			.isSameAs(secondFailure);

		verify(externalTaskServiceMock, times(2)).handleFailure("id", "worker", "message", 2, 10L, VARIABLES, LOCAL_VARIABLES);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void serverErrorOnCompleteIsRethrownWithoutRetryOrEviction() {
		final var failure = new EngineException("Internal error", restException(500));
		doThrow(failure).when(externalTaskServiceMock).complete(externalTaskMock);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(EngineException.class)
			.isThrownBy(() -> externalTaskService.complete(externalTaskMock))
			.isSameAs(failure);

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void connectionLostOnCompleteIsRethrownWithoutRetryOrEviction() {
		final var failure = new ConnectionLostException("Connection lost", new IOException());
		doThrow(failure).when(externalTaskServiceMock).complete(externalTaskMock);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(ConnectionLostException.class)
			.isThrownBy(() -> externalTaskService.complete(externalTaskMock))
			.isSameAs(failure);

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void errorFromServiceIsRethrownAsItselfWithoutRetryOrEviction() {
		final var failure = new OutOfMemoryError("out of memory");
		doThrow(failure).when(externalTaskServiceMock).complete(externalTaskMock);
		final var externalTaskService = wrap();

		assertThatExceptionOfType(OutOfMemoryError.class)
			.isThrownBy(() -> externalTaskService.complete(externalTaskMock))
			.isSameAs(failure);

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoMoreInteractions(externalTaskServiceMock);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void unauthorizedOnExtendLockEvictsTokenAndRetriesOnce() {
		doThrow(unauthorized()).doNothing().when(externalTaskServiceMock).extendLock(externalTaskMock, 1000L);

		wrap().extendLock(externalTaskMock, 1000L);

		verify(externalTaskServiceMock, times(2)).extendLock(externalTaskMock, 1000L);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	@Test
	void unauthorizedOnUnlockEvictsTokenAndRetriesOnce() {
		doThrow(unauthorized()).doNothing().when(externalTaskServiceMock).unlock(externalTaskMock);

		wrap().unlock(externalTaskMock);

		verify(externalTaskServiceMock, times(2)).unlock(externalTaskMock);
		verify(tokenMock).evict();
		verifyNoMoreInteractions(externalTaskServiceMock, tokenMock);
	}

	private ExternalTaskService wrap() {
		return UnauthorizedRetryingExternalTaskService.wrap(externalTaskServiceMock, tokenMock);
	}

	private static UnknownHttpErrorException unauthorized() {
		return new UnknownHttpErrorException("Unauthorized", restException(401));
	}

	private static RestException restException(final int httpStatusCode) {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(httpStatusCode);
		return restException;
	}
}
