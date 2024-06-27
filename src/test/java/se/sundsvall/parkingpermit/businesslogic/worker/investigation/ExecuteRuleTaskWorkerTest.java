package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.LOST_PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT_RENEWAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.businessrules.BusinessRulesClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class ExecuteRuleTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final long ERRAND_ID = 123L;
	private static final String KEY_RULE_ENGINE_RESPONSE = "ruleEngineResponse";

	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";
	private static final String VARIABLE_MUNICIPALITY_ID = "municipalityId";
	private static final String KEY_APPLICATION_APPLICANT_CAPACITY = "applicationApplicantCapacity";
	private static final String KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES = "applicationRenewalChangedCircumstances";
	private static final String KEY_DISABILITY_DURATION = "disabilityDuration";
	private static final String KEY_DISABILITY_WALKING_ABILITY = "disabilityWalkingAbility";
	private static final String KEY_DISABILITY_WALKING_DISTANCE_MAX = "disabilityWalkingDistanceMax";
	private static final String KEY_LOST_PERMIT_POLICE_REPORT_NUMBER = "lostPermitPoliceReportNumber";

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private BusinessRulesClient businessRulesClientMock;

	@Mock
	private ErrandDTO errandMock;

	@Mock
	private StakeholderDTO stakeholderMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private ExecuteRulesTaskWorker worker;

	@Test
	void executeWhenNewParkingPermit() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT);
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			KEY_APPLICATION_APPLICANT_CAPACITY, "applicantCapacity",
			KEY_DISABILITY_DURATION, "disabilityDuration",
			KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
			KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"));

		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(KEY_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verify(externalTaskMock).getVariable(VARIABLE_REQUEST_ID);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenRenewalParkingPermit() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			KEY_APPLICATION_APPLICANT_CAPACITY, "applicantCapacity",
			KEY_DISABILITY_DURATION, "disabilityDuration",
			KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES, "changedCircumstances",
			KEY_DISABILITY_WALKING_ABILITY, "walkingAbility",
			KEY_DISABILITY_WALKING_DISTANCE_MAX, "walkingDistanceMax"));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(KEY_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verify(externalTaskMock).getVariable(VARIABLE_REQUEST_ID);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenLostParkingPermit() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(LOST_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			KEY_LOST_PERMIT_POLICE_REPORT_NUMBER, "policeReportNumber"));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(KEY_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verify(externalTaskMock).getVariable(VARIABLE_REQUEST_ID);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(LOST_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(Map.of(
			KEY_LOST_PERMIT_POLICE_REPORT_NUMBER, "policeReportNumber"));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(KEY_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verify(externalTaskMock).getVariable(VARIABLE_REQUEST_ID);
	}
}
