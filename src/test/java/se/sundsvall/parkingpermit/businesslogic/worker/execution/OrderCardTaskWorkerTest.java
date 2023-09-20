package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum;
import generated.se.sundsvall.casedata.StatusDTO;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.RpaService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.LOST_PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT_RENEWAL;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

@ExtendWith(MockitoExtension.class)
class OrderCardTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";
	private static final String QUEUE_NEW_CARD = "NyttKortNyPerson";
	private static final String QUEUE_REPLACEMENT_CARD = "NyttKortBefintligPerson";
	private static final String QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD = "StöldspärraSamtSkapaNyttKort";
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
	private ArgumentCaptor<List<StatusDTO>> statusesDTOArgumentCaptor;

	@InjectMocks
	private OrderCardTaskWorker worker;

	@ParameterizedTest
	@MethodSource("orderCardTypeArguments")
	void execute(CaseTypeEnum caseType, List<String> expectedQueueNames) {
		//Arrange
		final var errand = new ErrandDTO().id(ERRAND_ID).caseType(caseType);
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(rpaServiceMock).addQueueItems(expectedQueueNames, ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(ERRAND_ID), statusesDTOArgumentCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		assertThat(statusesDTOArgumentCaptor.getValue()).hasSize(1)
			.extracting(StatusDTO::getStatusType, StatusDTO::getDescription)
			.containsExactly(
				tuple(CASEDATA_STATUS_DECISION_EXECUTED, CASEDATA_STATUS_DECISION_EXECUTED));
		assertThat(statusesDTOArgumentCaptor.getValue().get(0).getDateTime()).isCloseTo(OffsetDateTime.now(), within(1, SECONDS));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var errand = new ErrandDTO().id(ERRAND_ID).caseType(PARKING_PERMIT);
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errand);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		// Mock
		doThrow(thrownException).when(rpaServiceMock).addQueueItems(any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
	}

	private static Stream<Arguments> orderCardTypeArguments() {
		return Stream.of(
			Arguments.of(PARKING_PERMIT, List.of(QUEUE_NEW_CARD)),
					Arguments.of(PARKING_PERMIT_RENEWAL, List.of(QUEUE_REPLACEMENT_CARD)),
					Arguments.of(LOST_PARKING_PERMIT, List.of(QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD)));
	}
}
