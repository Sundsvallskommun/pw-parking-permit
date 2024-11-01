package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.businessrules.BusinessRulesClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static se.sundsvall.parkingpermit.Constants.*;

@ExtendWith(MockitoExtension.class)
class ExecuteRuleTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKINGPERMIT";
	private static final long ERRAND_ID = 123L;

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
	private Errand errandMock;

	@Mock
	private Stakeholder stakeholderMock;

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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT);
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("applicantCapacity"),
			new ExtraParameter(KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			new ExtraParameter(KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("walkingDistanceMax")));

		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenRenewalParkingPermit() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_PARKING_PERMIT_RENEWAL);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(KEY_APPLICATION_APPLICANT_CAPACITY).addValuesItem("applicantCapacity"),
			new ExtraParameter(KEY_DISABILITY_DURATION).addValuesItem("disabilityDuration"),
			new ExtraParameter(KEY_APPLICATION_RENEWAL_CHANGED_CIRCUMSTANCES).addValuesItem("changedCircumstances"),
			new ExtraParameter(KEY_DISABILITY_WALKING_ABILITY).addValuesItem("walkingAbility"),
			new ExtraParameter(KEY_DISABILITY_WALKING_DISTANCE_MAX).addValuesItem("walkingDistanceMax")));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenLostParkingPermit() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_LOST_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(KEY_LOST_PERMIT_POLICE_REPORT_NUMBER).addValuesItem("policeReportNumber")));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(stakeholderMock.getPersonId()).thenReturn("personId");
		when(stakeholderMock.getRoles()).thenReturn(List.of(ROLE_APPLICANT));
		when(errandMock.getStakeholders()).thenReturn(List.of(stakeholderMock));
		when(errandMock.getCaseType()).thenReturn(CASE_TYPE_LOST_PARKING_PERMIT);
		when(errandMock.getExtraParameters()).thenReturn(List.of(
			new ExtraParameter(KEY_LOST_PERMIT_POLICE_REPORT_NUMBER).addValuesItem("policeReportNumber")));
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(errandMock).getStakeholders();
		verify(errandMock, times(2)).getCaseType();
		verify(stakeholderMock).getPersonId();
		verify(stakeholderMock).getRoles();
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));

	}
}
