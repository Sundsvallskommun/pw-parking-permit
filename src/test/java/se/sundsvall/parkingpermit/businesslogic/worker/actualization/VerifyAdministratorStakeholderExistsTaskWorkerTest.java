package se.sundsvall.parkingpermit.businesslogic.worker.actualization;


import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.PatchErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.FALSE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;

@ExtendWith(MockitoExtension.class)
class VerifyAdministratorStakeholderExistsTaskWorkerTest {

    private static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String REQUEST_ID = "RequestId";
    private static final long ERRAND_ID = 123L;
    private static final String MUNICIPALITY_ID = "2281";
    private static final String DISPLAY_PHASE = "displayPhase";

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

    @Mock
    private StakeholderDTO stakeholderMock;

    @InjectMocks
    private VerifyAdministratorStakeholderExistsTaskWorker worker;

    @Captor
    private ArgumentCaptor<PatchErrandDTO> patchCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    @BeforeEach
    void commonMocking() {
        when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
        when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
        when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
        when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
        when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, ERRAND_ID)).thenReturn(errandMock);
        when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
        when(errandMock.getId()).thenReturn(ERRAND_ID);
    }

    @Test
    void executeWhenErrandHasAdministratorAssigned() {
        // Arrange
        when(errandMock.getExtraParameters()).thenReturn(Map.of(CASEDATA_KEY_DISPLAY_PHASE, DISPLAY_PHASE));
        when(stakeholderMock.getRoles()).thenReturn(List.of("ADMINISTRATOR"));

        // Act
        worker.execute(externalTaskMock, externalTaskServiceMock);

        // Assert and verify
        verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
        verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, ERRAND_ID);
        verify(errandMock).getStakeholders();
        verify(errandMock).getId();
        verify(errandMock).getExtraParameters();
        verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
        verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
        verifyNoInteractions(failureHandlerMock);

        assertThat(variablesCaptor.getValue()).containsExactlyEntriesOf(
                Map.of(CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR, true));
    }

    @Test
    void executeWhenErrandHasNoAdministratorAssigned() {
        // Arrange
        final var externalCaseId = UUID.randomUUID().toString();
        final var phase = "phase";

        when(errandMock.getExtraParameters()).thenReturn(Map.of(CASEDATA_KEY_DISPLAY_PHASE, DISPLAY_PHASE));
        when(errandMock.getExternalCaseId()).thenReturn(externalCaseId);
        when(errandMock.getPhase()).thenReturn(phase);

        // Act
        worker.execute(externalTaskMock, externalTaskServiceMock);

        // Assert and verify
        verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
        verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, ERRAND_ID);
        verify(errandMock).getStakeholders();
        verify(errandMock, times(2)).getId();
        verify(errandMock).getExtraParameters();
        verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(ERRAND_ID), patchCaptor.capture());
        verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
        verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
        verifyNoInteractions(failureHandlerMock);

        assertThat(patchCaptor.getValue()).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase", "extraParameters").satisfies(patch -> {
            assertThat(patch.getExternalCaseId()).isEqualTo(externalCaseId);
            assertThat(patch.getPhase()).isEqualTo(phase);
            assertThat(patch.getExtraParameters()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    CASEDATA_KEY_PHASE_STATUS, PHASE_STATUS_WAITING,
                    CASEDATA_KEY_PHASE_ACTION, PHASE_ACTION_UNKNOWN));
        });

        assertThat(variablesCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(
                Map.of(CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR, false,
                        CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING));
    }

    @Test
    void executeWhenCanceled() {
        // Arrange
        final var externalCaseId = UUID.randomUUID().toString();
        final var phase = "phase";

        when(errandMock.getExtraParameters()).thenReturn(Map.of(CASEDATA_KEY_DISPLAY_PHASE, DISPLAY_PHASE, CASEDATA_KEY_PHASE_ACTION, PHASE_ACTION_CANCEL));
        when(errandMock.getExternalCaseId()).thenReturn(externalCaseId);
        when(errandMock.getPhase()).thenReturn(phase);

        // Act
        worker.execute(externalTaskMock, externalTaskServiceMock);

        // Assert and verify
        verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
        verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, ERRAND_ID);
        verify(errandMock).getStakeholders();
        verify(errandMock, times(3)).getId();
        verify(errandMock).getExtraParameters();
        verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(ERRAND_ID), patchCaptor.capture());
        verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
        verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
        verifyNoInteractions(failureHandlerMock);

        assertThat(patchCaptor.getValue()).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase", "extraParameters").satisfies(patch -> {
            assertThat(patch.getExternalCaseId()).isEqualTo(externalCaseId);
            assertThat(patch.getPhase()).isEqualTo(phase);
            assertThat(patch.getExtraParameters()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    CASEDATA_KEY_PHASE_STATUS, PHASE_STATUS_CANCELED,
                    CASEDATA_KEY_PHASE_ACTION, PHASE_ACTION_CANCEL));
        });

        assertThat(variablesCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR, false,
                CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL,
                CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED));
    }

    @Test
    void executeThrowsException() {
        // Arrange
        final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

        when(errandMock.getExtraParameters()).thenReturn(Map.of(CASEDATA_KEY_DISPLAY_PHASE, DISPLAY_PHASE));
        when(stakeholderMock.getRoles()).thenReturn(List.of("ADMINISTRATOR"));

        doThrow(problem).when(externalTaskServiceMock).complete(any(), any());

        // Act
        worker.execute(externalTaskMock, externalTaskServiceMock);

        // Assert and verify
        verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
        verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
        verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, ERRAND_ID);
        verify(errandMock).getStakeholders();
        verify(errandMock).getId();
        verify(errandMock).getExtraParameters();
        verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());

        assertThat(variablesCaptor.getValue()).containsExactlyEntriesOf(
                Map.of(CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR, true));

        // Verify failure handling
        verify(externalTaskMock).getId();
        verify(externalTaskMock).getBusinessKey();
        verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
        verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
        verifyNoMoreInteractions(externalTaskServiceMock, camundaClientMock, caseDataClientMock);
    }
}