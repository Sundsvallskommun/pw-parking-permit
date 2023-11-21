package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.casedata.StatusDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.ANMALAN_INSTALLATION_VARMEPUMP;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_AWAITING_COMPLETION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_PROCESS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_RECEIVED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_COMPLETION_RECEIVED;

@ExtendWith(MockitoExtension.class)
class SanityTestTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String VARIABLE_SANITY_CHECK_PASSED = "sanityCheckPassed";
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

	@Mock
	private ErrandDTO errandMock;

	@Mock
	private StakeholderDTO stakeholderMock;

	@InjectMocks
	private SanityCheckTaskWorker worker;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("InvestigationSanityCheckTask");
	}

	@ParameterizedTest
	@MethodSource("sanityChecksTypeArguments")
	void execute(List<RolesEnum> roles, CaseTypeEnum caseType, List<StatusDTO> statuses, boolean expectedSanityCheckPassed) {
		// Setup
		final var processInstanceId = "processInstanceId";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(processInstanceId);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getStatuses()).thenReturn(statuses);
		lenient().when(errandMock.getCaseType()).thenReturn(caseType);
		lenient().when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		lenient().when(stakeholderMock.getRoles()).thenReturn(roles);
		lenient().when(stakeholderMock.getType()).thenReturn(PERSON);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(camundaClientMock).setProcessInstanceVariable(processInstanceId, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(VARIABLE_SANITY_CHECK_PASSED, expectedSanityCheckPassed));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");

		// Mock to simulate exception upon patching errand with new phase
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		doThrow(problem).when(externalTaskServiceMock).complete(any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(errandMock, times(2)).getId();
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
	}

	private static Stream<Arguments> sanityChecksTypeArguments() {
		return Stream.of(
			// Sanity check passes
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), PARKING_PERMIT, List.of(new StatusDTO().statusType(CASEDATA_STATUS_CASE_RECEIVED)), true),
			//Sanity check passes
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), PARKING_PERMIT, List.of(new StatusDTO().statusType(CASEDATA_STATUS_COMPLETION_RECEIVED), new StatusDTO().statusType(CASEDATA_STATUS_CASE_PROCESS)), true),
			//Sanity check fails when no administrator
			Arguments.of(List.of(APPLICANT), PARKING_PERMIT, List.of(new StatusDTO().statusType(CASEDATA_STATUS_COMPLETION_RECEIVED)), false),
			//Sanity check fails when no applicant
			Arguments.of(List.of(ADMINISTRATOR), PARKING_PERMIT, List.of(new StatusDTO().statusType(CASEDATA_STATUS_COMPLETION_RECEIVED)), false),
			//Sanity check fails when no status
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), PARKING_PERMIT, emptyList(), false),
			//Sanity check fails when wrong status
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), PARKING_PERMIT, List.of(new StatusDTO().statusType(CASEDATA_STATUS_AWAITING_COMPLETION)), false),
			//Sanity check fails when wrong case type
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), ANMALAN_INSTALLATION_VARMEPUMP, List.of(new StatusDTO().statusType(CASEDATA_STATUS_COMPLETION_RECEIVED)), false),
			//Sanity check fails when no case type
			Arguments.of(List.of(ADMINISTRATOR, APPLICANT), null, List.of(new StatusDTO().statusType(CASEDATA_STATUS_COMPLETION_RECEIVED)), false));
	}
}
