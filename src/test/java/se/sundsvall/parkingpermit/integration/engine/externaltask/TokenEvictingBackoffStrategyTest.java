package se.sundsvall.parkingpermit.integration.engine.externaltask;

import java.io.IOException;
import java.util.List;
import org.camunda.bpm.client.exception.ConnectionLostException;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
import org.camunda.bpm.client.task.ExternalTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TokenEvictingBackoffStrategyTest {

	@Mock
	private OperatonExternalTaskToken tokenMock;

	private TokenEvictingBackoffStrategy strategy;

	@BeforeEach
	void setUp() {
		strategy = new TokenEvictingBackoffStrategy(500, 2, 15000, tokenMock);
	}

	@Test
	void emptyPollWithoutErrorGivesNoBackoff() {
		strategy.reconfigure(List.of(), null);

		assertThat(strategy.calculateBackoffTime()).isZero();
		verifyNoInteractions(tokenMock);
	}

	@Test
	void repeatedEmptyPollsWithoutErrorGiveNoBackoff() {
		for (var i = 0; i < 5; i++) {
			strategy.reconfigure(List.of(), null);
			assertThat(strategy.calculateBackoffTime()).isZero();
		}
		verifyNoInteractions(tokenMock);
	}

	@Test
	void firstErrorGivesInitialBackoff() {
		strategy.reconfigure(List.of(), connectionLost());

		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void secondErrorInRowDoublesBackoff() {
		strategy.reconfigure(List.of(), connectionLost());
		strategy.reconfigure(List.of(), connectionLost());

		assertThat(strategy.calculateBackoffTime()).isEqualTo(1000);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void backoffNeverExceedsMaxTime() {
		// 500 * 2^5 = 16000 would exceed the max on the sixth error in a row
		for (var i = 0; i < 5; i++) {
			strategy.reconfigure(List.of(), connectionLost());
		}
		assertThat(strategy.calculateBackoffTime()).isEqualTo(8000);

		strategy.reconfigure(List.of(), connectionLost());
		assertThat(strategy.calculateBackoffTime()).isEqualTo(15000);

		for (var i = 0; i < 10; i++) {
			strategy.reconfigure(List.of(), connectionLost());
		}
		assertThat(strategy.calculateBackoffTime()).isEqualTo(15000);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void successfulPollAfterErrorsResetsBackoff() {
		strategy.reconfigure(List.of(), connectionLost());
		strategy.reconfigure(List.of(), connectionLost());

		strategy.reconfigure(List.of(mock(ExternalTask.class)), null);
		assertThat(strategy.calculateBackoffTime()).isZero();

		strategy.reconfigure(List.of(), connectionLost());
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
		verifyNoInteractions(tokenMock);
	}

	@Test
	void emptyPollWithoutErrorAfterErrorResetsBackoff() {
		strategy.reconfigure(List.of(), connectionLost());

		strategy.reconfigure(List.of(), null);

		assertThat(strategy.calculateBackoffTime()).isZero();
		verifyNoInteractions(tokenMock);
	}

	@Test
	void unauthorizedEvictsTokenAndBacksOff() {
		strategy.reconfigure(List.of(), new UnknownHttpErrorException("Unauthorized", restException(401)));

		verify(tokenMock).evict();
		verifyNoMoreInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void serverErrorBacksOffWithoutEviction() {
		strategy.reconfigure(List.of(), new EngineException("Internal error", restException(500)));

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void errorWithoutStatusBacksOffWithoutEviction() {
		strategy.reconfigure(List.of(), new RestException("No status", "type", 0));

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void connectionLostBacksOffWithoutEviction() {
		strategy.reconfigure(List.of(), connectionLost());

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	private static ConnectionLostException connectionLost() {
		return new ConnectionLostException("Connection lost", new IOException());
	}

	private static RestException restException(final int httpStatusCode) {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(httpStatusCode);
		return restException;
	}
}
