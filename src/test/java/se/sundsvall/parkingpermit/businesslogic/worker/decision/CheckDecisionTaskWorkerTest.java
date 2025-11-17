package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_RECEIVED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.Constants.FALSE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Status;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.joda.time.DateTime;
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
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
class CheckDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String EXTERNAL_CASE_ID = "externalCaseId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
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
	private SimplifiedServiceTextProperties simplifiedServiceTextPropertiesMock;

	@Mock
	private TextProvider textProviderMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private CheckDecisionTaskWorker worker;

	@Captor
	private ArgumentCaptor<PatchErrand> patchErrandCaptor;

	@Captor
	private ArgumentCaptor<List<ExtraParameter>> patchExtraParameterCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> mapCaptor;

	@Test
	void executeWhenDecisionIsDecidedAndApproved() {

		// Arrange
		final var status = new Status();
		status.setStatusType(CASEDATA_STATUS_CASE_DECIDED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(APPROVAL)));
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(KEY_PHASE_ACTION).addValuesItem("COMPLETE")));
		when(errandMock.getStatuses()).thenReturn(List.of(status));
		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(simplifiedServiceTextPropertiesMock.getDelay()).thenReturn("P1D");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(any(ExternalTask.class), mapCaptor.capture());
		assertThat(mapCaptor.getValue()).containsEntry(CAMUNDA_VARIABLE_FINAL_DECISION, true)
			.containsEntry(CAMUNDA_VARIABLE_IS_APPROVED, true);
		assertThat(mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE)).isInstanceOf(Date.class);
		assertThat(((Date) mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE))).isCloseTo(DateTime.now().plusDays(1).toDate(), 1000);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
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
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(KEY_PHASE_ACTION).addValuesItem("COMPLETE")));
		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(simplifiedServiceTextPropertiesMock.getDelay()).thenReturn("P1D");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(any(ExternalTask.class), mapCaptor.capture());
		assertThat(mapCaptor.getValue()).containsEntry(CAMUNDA_VARIABLE_FINAL_DECISION, true)
			.containsEntry(CAMUNDA_VARIABLE_IS_APPROVED, false);
		assertThat(mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE)).isInstanceOf(Date.class);
		assertThat(((Date) mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE))).isCloseTo(DateTime.now().plusDays(1).toDate(), 1000);

		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getExternalCaseId()).thenReturn(EXTERNAL_CASE_ID);
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
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verify(caseDataClientMock).patchErrandExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchExtraParameterCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue()).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase");
		assertThat(patchErrandCaptor.getValue().getExternalCaseId()).isEqualTo(EXTERNAL_CASE_ID);
		assertThat(patchErrandCaptor.getValue().getPhase()).isEqualTo("Beslut");
		assertThat(patchExtraParameterCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING)));
	}

	@Test
	void executeWhenDecisionIsNotFinal() {

		// Arrange
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_DECISION_EXECUTED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getExternalCaseId()).thenReturn(EXTERNAL_CASE_ID);
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
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verify(caseDataClientMock).patchErrandExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchExtraParameterCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue()).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase");
		assertThat(patchErrandCaptor.getValue().getExternalCaseId()).isEqualTo(EXTERNAL_CASE_ID);
		assertThat(patchErrandCaptor.getValue().getPhase()).isEqualTo("Beslut");
		assertThat(patchExtraParameterCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING)));
	}

	@Test
	void executeWhenDecisionsIsNull() {

		// Arrange
		final var status = new Status();
		status.statusType(CASEDATA_STATUS_DECISION_EXECUTED);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(null);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getExternalCaseId()).thenReturn(EXTERNAL_CASE_ID);
		when(errandMock.getStatuses()).thenReturn(List.of(status));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_FINAL_DECISION, false, CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING,
			CAMUNDA_VARIABLE_IS_APPROVED, false));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verify(caseDataClientMock).patchErrandExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchExtraParameterCaptor.capture());
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchErrandCaptor.getValue()).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "facilities", "phase", "relatesTo");
		assertThat(patchErrandCaptor.getValue().getExternalCaseId()).isEqualTo(EXTERNAL_CASE_ID);
		assertThat(patchErrandCaptor.getValue().getFacilities()).isNull();
		assertThat(patchErrandCaptor.getValue().getRelatesTo()).isNull();
		assertThat(patchErrandCaptor.getValue().getPhase()).isEqualTo("Beslut");
		assertThat(patchExtraParameterCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(CASEDATA_KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING)));
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
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
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
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
