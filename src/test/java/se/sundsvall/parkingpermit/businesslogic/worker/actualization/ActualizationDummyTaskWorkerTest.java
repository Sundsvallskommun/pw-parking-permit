package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

@ExtendWith(MockitoExtension.class)
class ActualizationDummyTaskWorkerTest {

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private ActualizationDummyTaskWorker worker;

	@Test
	void executeForCitizen() {
		// Mock
		when(externalTaskMock.getBusinessKey()).thenReturn("2");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("isResidentOfMunicipality", true));
		verify(externalTaskMock, times(2)).getBusinessKey();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeForNonCitizen() {
		// Mock
		when(externalTaskMock.getBusinessKey()).thenReturn("1");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("isResidentOfMunicipality", false));
		verify(externalTaskMock, times(2)).getBusinessKey();
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		when(externalTaskMock.getBusinessKey()).thenReturn("1");
		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("isResidentOfMunicipality", false));
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock, times(3)).getBusinessKey();
		verifyNoInteractions(camundaClientMock);
	}
}
