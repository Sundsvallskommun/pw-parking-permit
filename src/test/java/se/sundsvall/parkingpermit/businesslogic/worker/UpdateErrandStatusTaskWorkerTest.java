package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StatusDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_PROCESS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;

@ExtendWith(MockitoExtension.class)
class UpdateErrandStatusTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;

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

	@Mock
	private ErrandDTO errandMock;

	@InjectMocks
	private UpdateErrandStatusTaskWorker worker;

	@Captor
	private ArgumentCaptor<List<StatusDTO>> statusCaptor;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("UpdateErrandStatusTask");
	}

	@Test
	void executeWhenPhaseInvestigate() {
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Utredning");
		when(errandMock.getStatuses()).thenReturn(new ArrayList<>());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().get(0).getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().get(0).getDescription()).isEqualTo("Ärendet utreds");
		assertThat(statusCaptor.getValue().get(0).getStatusType()).isEqualTo(CASEDATA_STATUS_CASE_PROCESS);
	}

	@Test
	void executeWhenPhaseDecisionAndCitizen() {
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(false);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Beslut");
		when(errandMock.getStatuses()).thenReturn(new ArrayList<>());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().get(0).getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().get(0).getDescription()).isEqualTo("Ärendet beslutas");
		assertThat(statusCaptor.getValue().get(0).getStatusType()).isEqualTo(CASEDATA_STATUS_CASE_DECIDE);
	}

	@Test
	void executeWhenPhaseDecisionAndNotCitizen() {
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(true);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Beslut");
		when(errandMock.getStatuses()).thenReturn(new ArrayList<>());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().get(0).getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().get(0).getDescription()).isEqualTo("Ärendet avvisas");
		assertThat(statusCaptor.getValue().get(0).getStatusType()).isEqualTo(CASEDATA_STATUS_DECISION_EXECUTED);
	}


	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon updating errand with new status
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(true);
		when(errandMock.getPhase()).thenReturn("Beslut");
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.putStatus(any(), any())).thenThrow(problem);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verifyNoInteractions(camundaClientMock);
	}
}
