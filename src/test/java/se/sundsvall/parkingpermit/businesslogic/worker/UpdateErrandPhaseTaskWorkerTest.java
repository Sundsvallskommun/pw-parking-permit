package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.PatchErrandDTO;
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

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

@ExtendWith(MockitoExtension.class)
class UpdateErrandPhaseTaskWorkerTest {

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
	private UpdateErrandPhaseTaskWorker worker;

	@Captor
	private ArgumentCaptor<PatchErrandDTO> patchErrandCaptor;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("UpdateErrandPhaseTask");
	}

	@Test
	void execute() {
		// Setup
		final var externalCaseId = "externalCaseId";
		// Sets phase action to unknown in UpdateErrandPhaseTaskWorker because it is the beginning of the phase
		final var variables = new HashMap<String, Object>();
		variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_UNKNOWN);


		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_PHASE)).thenReturn(CASEDATA_PHASE_DECISION);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getExternalCaseId()).thenReturn(externalCaseId);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_PHASE);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).patchErrand(eq(ERRAND_ID), patchErrandCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock, variables);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		assertThat(patchErrandCaptor.getValue().getExternalCaseId()).isEqualTo(externalCaseId);
		assertThat(patchErrandCaptor.getValue().getPhase()).isEqualTo(CASEDATA_PHASE_DECISION);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon patching errand with new phase
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_PHASE)).thenReturn(CASEDATA_PHASE_DECISION);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.patchErrand(any(), any())).thenThrow(problem);

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
