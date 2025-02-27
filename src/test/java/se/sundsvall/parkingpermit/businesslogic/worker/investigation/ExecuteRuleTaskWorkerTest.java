package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE;

import generated.se.sundsvall.businessrules.RuleEngineRequest;
import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Errand;
import java.util.ArrayList;
import java.util.Map;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.businessrules.BusinessRulesClient;
import se.sundsvall.parkingpermit.integration.businessrules.mapper.BusinessRulesMapper;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class ExecuteRuleTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final long ERRAND_ID = 123L;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private BusinessRulesClient businessRulesClientMock;

	@Mock
	private Errand errandMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private ExecuteRulesTaskWorker worker;

	@Test
	void executeBusinessLogic() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		final var ruleEngineRequest = new RuleEngineRequest();
		final var attachmentList = new ArrayList<Attachment>();
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(caseDataClientMock.getErrandAttachments(any(), any(), any())).thenReturn(attachmentList);
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		try (MockedStatic<BusinessRulesMapper> mapper = Mockito.mockStatic(BusinessRulesMapper.class)) {
			mapper.when(() -> BusinessRulesMapper.toRuleEngineRequest(any(), any())).thenReturn(ruleEngineRequest);
			worker.execute(externalTaskMock, externalTaskServiceMock);

			mapper.verify(() -> BusinessRulesMapper.toRuleEngineRequest(same(errandMock), same(attachmentList)));
		}

		// Act

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).getErrandAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), same(ruleEngineRequest));
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verifyNoInteractions(failureHandlerMock);
		verifyNoMoreInteractions(caseDataClientMock, businessRulesClientMock, errandMock, externalTaskMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse();
		final var ruleEngineRequest = new RuleEngineRequest();
		final var attachmentList = new ArrayList<Attachment>();
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(caseDataClientMock.getErrandAttachments(any(), any(), any())).thenReturn(attachmentList);
		when(businessRulesClientMock.runRuleEngine(any(), any())).thenReturn(ruleEngineResponse);

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		try (MockedStatic<BusinessRulesMapper> mapper = Mockito.mockStatic(BusinessRulesMapper.class)) {
			mapper.when(() -> BusinessRulesMapper.toRuleEngineRequest(any(), any())).thenReturn(ruleEngineRequest);
			worker.execute(externalTaskMock, externalTaskServiceMock);

			mapper.verify(() -> BusinessRulesMapper.toRuleEngineRequest(same(errandMock), same(attachmentList)));
		}

		// Assert and verify
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(businessRulesClientMock).runRuleEngine(eq(MUNICIPALITY_ID), any());
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).getErrandAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse));
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "TestException");
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verifyNoMoreInteractions(caseDataClientMock, businessRulesClientMock, errandMock, externalTaskMock, failureHandlerMock);
	}
}
