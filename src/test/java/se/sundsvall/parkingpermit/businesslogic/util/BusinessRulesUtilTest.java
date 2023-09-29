package se.sundsvall.parkingpermit.businesslogic.util;

import generated.se.sundsvall.businessrules.Result;
import generated.se.sundsvall.businessrules.ResultDetail;
import generated.se.sundsvall.businessrules.ResultValue;
import generated.se.sundsvall.casedata.DecisionDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.RECOMMENDED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BusinessRulesUtilTest {


	@ParameterizedTest
	@MethodSource("constructDecisionTypeArguments")
	void constructDecision(Result resultFromRuleEngine, DecisionDTO expectedDecisionDTO) {

		final var decisionDTO = BusinessRulesUtil.constructDecision(resultFromRuleEngine);

		assertThat(decisionDTO).isNotNull();
		assertThat(decisionDTO.getDecisionType()).isEqualTo(expectedDecisionDTO.getDecisionType());
		assertThat(decisionDTO.getDecisionOutcome()).isEqualTo(expectedDecisionDTO.getDecisionOutcome());
		assertThat(decisionDTO.getDescription()).isEqualTo(expectedDecisionDTO.getDescription());
		assertThat(decisionDTO.getCreated()).isCloseTo(expectedDecisionDTO.getCreated(), within(1, SECONDS));
	}

	static Stream<Arguments> constructDecisionTypeArguments() {
		return Stream.of(
			Arguments.of(createRuleEngineResult("PASS"), new DecisionDTO()
																		.decisionType(RECOMMENDED)
																		.decisionOutcome(APPROVAL)
																		.description("Rekommenderat beslut är bevilja. Description1, description2 och description3.")
																		.created(OffsetDateTime.now())),
			Arguments.of(createRuleEngineResult("FAIL"), new DecisionDTO()
																		.decisionType(RECOMMENDED)
																		.decisionOutcome(REJECTION)
																		.description("Rekommenderat beslut är avslag. Description1, description2 och description3.")
																		.created(OffsetDateTime.now()))
		);
	}

	private static Result createRuleEngineResult(String resultValue) {
		return new Result().value(ResultValue.fromValue(resultValue))
			.details(List.of(new ResultDetail().description("description1"), new ResultDetail().description("description2"), new ResultDetail().description("description3")));
	}
}
