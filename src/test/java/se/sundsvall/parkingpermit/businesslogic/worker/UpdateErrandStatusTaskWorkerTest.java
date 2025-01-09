package se.sundsvall.parkingpermit.businesslogic.worker;

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
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_ACTUALIZATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_PROCESS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Status;
import java.util.List;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class UpdateErrandStatusTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

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
	private Errand errandMock;

	@InjectMocks
	private UpdateErrandStatusTaskWorker worker;

	@Captor
	private ArgumentCaptor<List<Status>> statusCaptor;

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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Utredning");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().getFirst().getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().getFirst().getDescription()).isEqualTo("Ärendet utreds");
		assertThat(statusCaptor.getValue().getFirst().getStatusType()).isEqualTo(CASEDATA_STATUS_CASE_PROCESS);
	}

	@Test
	void executeWhenPhaseDecisionAndCitizen() {
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(false);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Beslut");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().getFirst().getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().getFirst().getDescription()).isEqualTo("Ärendet beslutas");
		assertThat(statusCaptor.getValue().getFirst().getStatusType()).isEqualTo(CASEDATA_STATUS_CASE_DECIDE);
	}

	@Test
	void executeWhenPhaseDecisionAndNotCitizen() {
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(true);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn("Beslut");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().getFirst().getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().getFirst().getDescription()).isEqualTo("Ärendet avvisas");
		assertThat(statusCaptor.getValue().getFirst().getStatusType()).isEqualTo(CASEDATA_STATUS_DECISION_EXECUTED);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"testStatusDescription"
	})
	void executeWhenPhaseActualization(String statusDescription) {

		final String status = "testStatus";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getVariable("status")).thenReturn(status);
		when(externalTaskMock.getVariable("statusDescription")).thenReturn(statusDescription);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getPhase()).thenReturn(CASEDATA_PHASE_ACTUALIZATION);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).putStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), statusCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(statusCaptor.getValue().size()).isOne();
		assertThat(statusCaptor.getValue().getFirst().getDateTime()).isCloseTo(now(), within(2, SECONDS));
		assertThat(statusCaptor.getValue().getFirst().getDescription()).isEqualTo(statusDescription == null ? status : statusDescription);
		assertThat(statusCaptor.getValue().getFirst().getStatusType()).isEqualTo(status);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(org.zalando.problem.Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon updating errand with new status
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY)).thenReturn(true);
		when(errandMock.getPhase()).thenReturn("Beslut");
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.putStatus(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any())).thenThrow(problem);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert

		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verifyNoInteractions(camundaClientMock);
	}
}
