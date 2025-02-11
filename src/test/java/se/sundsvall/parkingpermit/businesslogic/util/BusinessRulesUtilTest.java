package se.sundsvall.parkingpermit.businesslogic.util;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import generated.se.sundsvall.businessrules.Result;
import generated.se.sundsvall.businessrules.ResultDetail;
import generated.se.sundsvall.businessrules.ResultValue;
import generated.se.sundsvall.casedata.Decision;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BusinessRulesUtilTest {

	@ParameterizedTest
	@MethodSource("constructDecisionTypeArguments")
	void constructDecision(boolean isAutomatic, Result resultFromRuleEngine, Decision expectedDecision) {

		final var decision = BusinessRulesUtil.constructDecision(resultFromRuleEngine, isAutomatic);

		assertThat(decision).isNotNull();
		assertThat(decision.getDecisionType()).isEqualTo(expectedDecision.getDecisionType());
		assertThat(decision.getDecisionOutcome()).isEqualTo(expectedDecision.getDecisionOutcome());
		assertThat(decision.getDescription()).isEqualTo(expectedDecision.getDescription());
		assertThat(decision.getCreated()).isCloseTo(expectedDecision.getCreated(), within(1, SECONDS));
	}

	static Stream<Arguments> constructDecisionTypeArguments() {
		return Stream.of(
			Arguments.of(false, createRuleEngineResult("PASS"), new Decision()
				.decisionType(RECOMMENDED)
				.decisionOutcome(APPROVAL)
				.description("Rekommenderat beslut är bevilja. Description1, description2 och description3.")
				.created(OffsetDateTime.now())),
			Arguments.of(false, createRuleEngineResult("FAIL"), new Decision()
				.decisionType(RECOMMENDED)
				.decisionOutcome(REJECTION)
				.description("Rekommenderat beslut är avslag. Description1, description2 och description3.")
				.created(OffsetDateTime.now())),
			Arguments.of(true, createRuleEngineResult("PASS"), new Decision()
				.decisionType(FINAL)
				.decisionOutcome(APPROVAL)
				.description("Beslut är bevilja. Description1, description2 och description3.")
				.created(OffsetDateTime.now())),
			Arguments.of(true, createRuleEngineResult("FAIL"), new Decision()
				.decisionType(FINAL)
				.decisionOutcome(REJECTION)
				.description("Beslut är avslag. Description1, description2 och description3.")
				.created(OffsetDateTime.now())));
	}

	private static Result createRuleEngineResult(String resultValue) {
		return new Result().value(ResultValue.fromValue(resultValue))
			.details(List.of(new ResultDetail().description("description1"), new ResultDetail().description("description2"), new ResultDetail().description("description3")));
	}
}
