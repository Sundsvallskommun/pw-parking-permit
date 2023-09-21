package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.ErrandDTO;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckCardExistsTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String PERMIT_NUMBER = "1234567890";
	private static final String PROCESS_INSTANCE_ID = "processInstanceId";
	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";
	private static final String CAMUNDA_VARIABLE_REQUEST_ID = "requestId";
	private static final String CAMUNDA_VARIABLE_UPDATE_AVAILABLE = "updateAvailable";
	private static final VariableValueDto FALSE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private CheckCardExistsTaskWorker worker;

	@Test
	void executeWhenCardExists() {
		//Arrange
		final var errand = new ErrandDTO()
			.id(ERRAND_ID)
			.extraParameters(Map.of("artefact.permit.number", PERMIT_NUMBER));
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("cardExists", true));
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenCardNotExists() {
		//Arrange
		final var errand = new ErrandDTO()
			.id(ERRAND_ID)
			.extraParameters(emptyMap());
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("cardExists", false));
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var errand = new ErrandDTO()
			.id(ERRAND_ID)
			.extraParameters(Map.of("artefact.permit.number", PERMIT_NUMBER));
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of("cardExists", true));
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
	}
}
