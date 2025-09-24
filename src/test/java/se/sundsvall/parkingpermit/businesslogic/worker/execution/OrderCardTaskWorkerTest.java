package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Status;
import java.time.OffsetDateTime;
import java.util.List;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.RpaService;

@ExtendWith(MockitoExtension.class)
class OrderCardTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final String QUEUE_PARKING_PERMIT = "ParkingPermit";
	private static final String CASEDATA_STATUS_DECISION_EXECUTED = "Beslut verkställt";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private RpaService rpaServiceMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Captor
	private ArgumentCaptor<Status> statusArgumentCaptor;

	@InjectMocks
	private OrderCardTaskWorker worker;

	@ParameterizedTest
	@ValueSource(strings = {
		CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT
	})
	void execute(String caseType) {
		// Arrange
		final var errand = new Errand().id(ERRAND_ID).caseType(caseType).namespace(NAMESPACE);

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskMock, times(2)).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(rpaServiceMock).addQueueItems(List.of(QUEUE_PARKING_PERMIT), ERRAND_ID, MUNICIPALITY_ID);
		verify(caseDataClientMock).patchStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), statusArgumentCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		assertThat(statusArgumentCaptor.getValue().getStatusType()).isEqualTo(CASEDATA_STATUS_DECISION_EXECUTED);
		assertThat(statusArgumentCaptor.getValue().getDescription()).isEqualTo(CASEDATA_STATUS_DECISION_EXECUTED);
		assertThat(statusArgumentCaptor.getValue().getCreated()).isCloseTo(OffsetDateTime.now(), within(1, SECONDS));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var errand = new Errand().id(ERRAND_ID).caseType(CASE_TYPE_PARKING_PERMIT);

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);

		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		doThrow(thrownException).when(rpaServiceMock).addQueueItems(any(), any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
	}
}
