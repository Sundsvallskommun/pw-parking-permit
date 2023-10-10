package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.ErrandDTO;
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
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

@ExtendWith(MockitoExtension.class)
class CreateAssetTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;

	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private PartyAssetsService partyAssetsServiceMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private CreateAssetTaskWorker worker;

	@Test
	void execute() {
		//Arrange
		final var errand = new ErrandDTO();
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(partyAssetsServiceMock).createAsset(errand);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		doThrow(thrownException).when(caseDataClientMock).getErrandById(ERRAND_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verifyNoInteractions(partyAssetsServiceMock);
	}
}
