package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.PatchErrand;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_FINALIZED;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_ONGOING;

@ExtendWith(MockitoExtension.class)
class UpdateErrandPhaseTaskWorkerTest {

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
	private UpdateErrandPhaseTaskWorker worker;

	@Captor
	private ArgumentCaptor<PatchErrand> patchErrandCaptor;

	@Captor
	private ArgumentCaptor<List<ExtraParameter>> patchExtraParameterCaptor;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, externalTaskMock, externalTaskServiceMock, failureHandlerMock);
	}

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("UpdateErrandPhaseTask");
	}

	private static Stream<Arguments> executePermutations() {
		return Stream.of(
			Arguments.of(CASEDATA_STATUS_CASE_FINALIZED, null),
			Arguments.of("OTHER_STATUS", null),
			Arguments.of(CASEDATA_STATUS_CASE_FINALIZED, PHASE_ACTION_COMPLETE),
			Arguments.of("OTHER_STATUS", PHASE_ACTION_COMPLETE),
			Arguments.of(CASEDATA_STATUS_CASE_FINALIZED, PHASE_ACTION_AUTOMATIC),
			Arguments.of("OTHER_STATUS", PHASE_ACTION_AUTOMATIC));
	}

	@ParameterizedTest
	@MethodSource("executePermutations")
	void execute(String status, String phaseAction) {
		// Setup
		final var externalCaseId = "externalCaseId";
		// Sets phase action to unknown or automatic in UpdateErrandPhaseTaskWorker because it is the beginning of the phase
		final var phaseActionPersist = PHASE_ACTION_AUTOMATIC.equals(phaseAction) ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN;
		final var variables = new HashMap<String, Object>();
		variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, phaseActionPersist);

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_PHASE)).thenReturn(CASEDATA_PHASE_DECISION);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getStatuses()).thenReturn(List.of(new generated.se.sundsvall.casedata.Status().statusType(status)));
		when(errandMock.getExtraParameters()).thenReturn(phaseAction == null ? null : List.of(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).values(List.of(phaseAction))));
		when(errandMock.getExternalCaseId()).thenReturn(externalCaseId);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_PHASE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchErrandCaptor.capture());
		verify(caseDataClientMock).patchErrandExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchExtraParameterCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock, variables);
		verifyNoInteractions(camundaClientMock, failureHandlerMock);

		final var patchErrand = patchErrandCaptor.getValue();

		assertThat(patchErrand).hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase");
		assertThat(patchErrand.getExternalCaseId()).isEqualTo(externalCaseId);
		assertThat(patchErrand.getPhase()).isEqualTo(CASEDATA_PHASE_DECISION);

		final var phaseStatus = CASEDATA_STATUS_CASE_FINALIZED.equals(status) ? PHASE_STATUS_COMPLETED : PHASE_STATUS_ONGOING;
		assertThat(patchExtraParameterCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues)
			.containsExactlyInAnyOrder(
				tuple(CASEDATA_KEY_PHASE_ACTION, List.of(phaseActionPersist)),
				tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(CASEDATA_PHASE_DECISION)),
				tuple(CASEDATA_KEY_PHASE_STATUS, List.of(phaseStatus)));
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon patching errand with new phase
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_PHASE)).thenReturn(CASEDATA_PHASE_DECISION);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.patchErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any())).thenThrow(problem);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE);
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verifyNoInteractions(camundaClientMock);
	}
}
