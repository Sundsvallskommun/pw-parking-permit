package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import generated.se.sundsvall.casedata.*;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static se.sundsvall.parkingpermit.Constants.*;

@ExtendWith(MockitoExtension.class)
class CheckDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKINGPERMIT";
	private static final String PROCESS_INSTANCE_ID = "processInstanceId";
	private static final String KEY_PHASE_ACTION = "process.phaseAction";

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private Errand errandMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private CheckDecisionTaskWorker worker;

	@Captor
	private ArgumentCaptor<PatchErrand> patchErrandCaptor;

	@Test
	void executeWhenDecisionIsDecidedAndApproved() {

		// Arrange
		final var status = new Status();
		status.setStatusType(CASEDATA_STATUS_CASE_DECIDED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(APPROVAL)));
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(KEY_PHASE_ACTION).addValuesItem("COMPLETE")));
		when(errandMock.getStatuses()).thenReturn(List.of(status));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, true, CAMUNDA_VARIABLE_IS_APPROVED, true));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verifyNoMoreInteractions(camundaClientMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenDecisionIsExecutedAndNotApproved() {

		// Arrange
		final var status = new Status();
		status.setStatusType(CASEDATA_STATUS_DECISION_EXECUTED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(createDecision(REJECTION)));
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(KEY_PHASE_ACTION).addValuesItem("COMPLETE")));
		when(errandMock.getStatuses()).thenReturn(List.of(status));
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(REJECTION)));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, true, CAMUNDA_VARIABLE_IS_APPROVED, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verifyNoMoreInteractions(camundaClientMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenDecisionIsNotTakenAndCancel() {

		// Arrange
		final var phaseActionCancel = "CANCEL";
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_CASE_RECEIVED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getDecisions()).thenReturn(List.of(createDecision(APPROVAL)));
		when(errandMock.getStatuses()).thenReturn(List.of(status));
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(KEY_PHASE_ACTION).addValuesItem(phaseActionCancel)));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, false, CAMUNDA_VARIABLE_PHASE_ACTION, phaseActionCancel,
			CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED, CAMUNDA_VARIABLE_IS_APPROVED, true));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue().getExtraParameters()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING))
			);
	}

	@Test
	void executeWhenDecisionIsNotFinal() {

		// Arrange
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_DECISION_EXECUTED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getDecisions()).thenReturn(List.of(createDecision(APPROVAL)));
		when(errandMock.getStatuses()).thenReturn(List.of(status));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, false, CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING,
			CAMUNDA_VARIABLE_IS_APPROVED, true));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue().getExtraParameters()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(CASEDATA_KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING))
			);
	}

	@Test
	void executeWhenDecisionsIsNull() {

		// Arrange
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_DECISION_EXECUTED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(null);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getStatuses()).thenReturn(List.of(status));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, false, CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING,
			CAMUNDA_VARIABLE_IS_APPROVED, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue().getExtraParameters()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(CASEDATA_KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING))
			);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_CASE_RECEIVED);

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getDecisions()).thenReturn(List.of(createDecision(REJECTION)));
		when(errandMock.getStatuses()).thenReturn(List.of(status));

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, false, CAMUNDA_VARIABLE_IS_APPROVED, false, CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verifyNoMoreInteractions(camundaClientMock);
	}

	private Decision createFinalDecision(DecisionOutcomeEnum decisionOutcome) {
		final var decision = new Decision();
		decision.setId(1L);
		decision.setDecisionType(FINAL);
		decision.setDecisionOutcome(decisionOutcome);
		return decision;
	}

	private Decision createDecision(DecisionOutcomeEnum decisionOutcome) {
		final var decision = new Decision();
		decision.setId(1L);
		decision.setDecisionType(RECOMMENDED);
		decision.setDecisionOutcome(decisionOutcome);
		return decision;
	}

}
