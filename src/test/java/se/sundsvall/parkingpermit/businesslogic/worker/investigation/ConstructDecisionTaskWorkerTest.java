package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static generated.se.sundsvall.businessrules.ResultValue.FAIL;
import static generated.se.sundsvall.businessrules.ResultValue.PASS;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE;

import generated.se.sundsvall.businessrules.Result;
import generated.se.sundsvall.businessrules.ResultDetail;
import generated.se.sundsvall.businessrules.ResultValue;
import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import java.util.List;
import java.util.stream.Stream;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
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
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class ConstructDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private CaseDataClient caseDataClientMock;
	@Mock
	private ExternalTask externalTaskMock;
	@Mock
	private ExternalTaskService externalTaskServiceMock;
	@Mock
	private Errand errandMock;
	@Mock
	private FailureHandler failureHandlerMock;

	@Captor
	private ArgumentCaptor<Decision> decisionArgumentCaptor;

	@Captor
	private ArgumentCaptor<Long> errandIdArgumentCaptor;

	@InjectMocks
	private ConstructDecisionTaskWorker worker;

	private static Stream<Arguments> constructDecisionTypeArguments() {
		return Stream.of(
			// Sanity check passes
			Arguments.of("PASS", new Decision().decisionType(RECOMMENDED).decisionOutcome(APPROVAL).description("Rekommenderat beslut är bevilja. Description1, description2 och description3.")),
			// Sanity check passes
			Arguments.of("FAIL", new Decision().decisionType(RECOMMENDED).decisionOutcome(REJECTION).description("Rekommenderat beslut är avslag. Description1, description2 och description3.")));
	}

	@ParameterizedTest
	@MethodSource("constructDecisionTypeArguments")
	void execute(String resultValue, Decision expectedDecision) {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse(resultValue);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getDecisions()).thenReturn(List.of(new Decision().decisionOutcome(REJECTION).decisionType(RECOMMENDED).version(0),
			new Decision().decisionOutcome(APPROVAL).decisionType(RECOMMENDED).version(1)));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).patchNewDecision(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandIdArgumentCaptor.capture(), decisionArgumentCaptor.capture());

		assertThat(errandIdArgumentCaptor.getValue()).isEqualTo(ERRAND_ID);
		assertThat(decisionArgumentCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionArgumentCaptor.getValue().getDecisionType()).isEqualTo(RECOMMENDED);
		assertThat(decisionArgumentCaptor.getValue().getDecisionOutcome()).isEqualTo(expectedDecision.getDecisionOutcome());
		assertThat(decisionArgumentCaptor.getValue().getDescription()).isEqualTo(expectedDecision.getDescription());
		assertThat(decisionArgumentCaptor.getValue().getVersion()).isEqualTo(2);

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenLatestDecisionIsEqual() {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse(FAIL.name());
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(new Decision()
			.decisionOutcome(REJECTION)
			.decisionType(RECOMMENDED)
			.description("Rekommenderat beslut är avslag. Description1, description2 och description3.").version(0)));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(caseDataClientMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsExceptionWhenNoPassOrFail() {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse("NOT_APPLICABLE");
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(emptyList());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Conflict: No applicable result found in rule engine response");
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(caseDataClientMock);
	}

	@Test
	void executeThrowsExceptionWhenNoResult() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse().results(emptyList());
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(emptyList());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Request: No results found in rule engine response");
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(caseDataClientMock);

	}

	@Test
	void executeThrowsExceptionWhenNullResponse() {

		// Arrange
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(null);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(emptyList());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Request: No rule engine response found");
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(caseDataClientMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse(PASS.toString());
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(emptyList());

		doThrow(thrownException).when(externalTaskServiceMock).complete(externalTaskMock);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
	}

	private RuleEngineResponse createRuleEngineResponse(String resultValue) {
		return new RuleEngineResponse().addResultsItem(new Result().value(ResultValue.fromValue(resultValue))
			.details(List.of(new ResultDetail().description("description1"), new ResultDetail().description("description2"), new ResultDetail().description("description3"))));
	}
}
