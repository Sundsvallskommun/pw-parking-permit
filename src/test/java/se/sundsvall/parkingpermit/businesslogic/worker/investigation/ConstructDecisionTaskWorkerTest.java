package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.businessrules.Result;
import generated.se.sundsvall.businessrules.ResultDetail;
import generated.se.sundsvall.businessrules.ResultValue;
import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.DecisionDTO;
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

import java.util.List;
import java.util.stream.Stream;

import static generated.se.sundsvall.businessrules.ResultValue.PASS;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.RECOMMENDED;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstructDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String KEY_RULE_ENGINE_RESPONSE = "ruleEngineResponse";

	private static final String VARIABLE_CASE_NUMBER = "caseNumber";
	private static final String VARIABLE_REQUEST_ID = "requestId";

	@Mock
	private CaseDataClient caseDataClientMock;
	@Mock
	private ExternalTask externalTaskMock;
	@Mock
	private ExternalTaskService externalTaskServiceMock;
	@Mock
	private FailureHandler failureHandlerMock;

	@Captor
	private ArgumentCaptor<DecisionDTO> decisionArgumentCaptor;

	@Captor
	private ArgumentCaptor<Long> errandIdArgumentCaptor;

	@InjectMocks
	private ConstructDecisionTaskWorker worker;

	@ParameterizedTest
	@MethodSource("constructDecisionTypeArguments")
	void execute(String resultValue, DecisionDTO expectedDecision) {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse(resultValue);
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(KEY_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(caseDataClientMock).patchNewDecision(errandIdArgumentCaptor.capture(), decisionArgumentCaptor.capture());

		assertThat(errandIdArgumentCaptor.getValue()).isEqualTo(ERRAND_ID);
		assertThat(decisionArgumentCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionArgumentCaptor.getValue().getDecisionType()).isEqualTo(RECOMMENDED);
		assertThat(decisionArgumentCaptor.getValue().getDecisionOutcome()).isEqualTo(expectedDecision.getDecisionOutcome());
		assertThat(decisionArgumentCaptor.getValue().getDescription()).isEqualTo(expectedDecision.getDescription());

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsExceptionWhenNoPassOrFail() {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse("NOT_APPLICABLE");
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(KEY_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Conflict: No applicable result found in rule engine response");
		verifyNoInteractions(caseDataClientMock);
	}

	@Test
	void executeThrowsExceptionWhenNoResult() {

		// Arrange
		final var ruleEngineResponse = new RuleEngineResponse().results(emptyList());
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(KEY_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Request: No results found in rule engine response");
		verifyNoInteractions(caseDataClientMock);
	}

	@Test
	void executeThrowsExceptionWhenNullResponse() {

		// Arrange
		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(KEY_RULE_ENGINE_RESPONSE)).thenReturn(null);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock, never()).complete(externalTaskMock);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Request: No rule engine response found");
		verifyNoInteractions(caseDataClientMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var ruleEngineResponse = createRuleEngineResponse(PASS.toString());
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));

		when(externalTaskMock.getVariable(VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(KEY_RULE_ENGINE_RESPONSE)).thenReturn(ruleEngineResponse);
		when(externalTaskMock.getVariable(VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);

		doThrow(thrownException).when(externalTaskServiceMock).complete(externalTaskMock);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verify(externalTaskMock).getVariable(VARIABLE_REQUEST_ID);
	}

	private static Stream<Arguments> constructDecisionTypeArguments() {
		return Stream.of(
			// Sanity check passes
			Arguments.of("PASS", new DecisionDTO().decisionType(RECOMMENDED).decisionOutcome(APPROVAL).description("Rekommenderat beslut är bevilja. Description1, description2 och description3.")),
			//Sanity check passes
			Arguments.of("FAIL", new DecisionDTO().decisionType(RECOMMENDED).decisionOutcome(REJECTION).description("Rekommenderat beslut är avslag. Description1, description2 och description3.")));
	}

	private RuleEngineResponse createRuleEngineResponse(String resultValue) {
		return new RuleEngineResponse().addResultsItem(new Result().value(ResultValue.fromValue(resultValue))
			.details(List.of(new ResultDetail().description("description1"), new ResultDetail().description("description2"), new ResultDetail().description("description3"))));
	}
}
