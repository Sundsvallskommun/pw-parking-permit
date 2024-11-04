package se.sundsvall.parkingpermit.businesslogic.util;

import com.google.re2j.Pattern;
import generated.se.sundsvall.businessrules.Result;
import generated.se.sundsvall.businessrules.ResultDetail;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.Decision.DecisionTypeEnum;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static generated.se.sundsvall.businessrules.ResultValue.PASS;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.dept44.util.DateUtils.toOffsetDateTimeWithLocalOffset;

public final class BusinessRulesUtil {

	static final String PREFIX_APPROVAL = "Rekommenderat beslut är bevilja. %s";
	static final String PREFIX_REJECT = "Rekommenderat beslut är avslag. %s";

	private static final String REGEXP_LAST_COMMA = "^(.*)(, )(.*)$";

	private BusinessRulesUtil() {}

	public static Decision constructDecision(Result resultFromRuleEngine) {
		if (isApproved(resultFromRuleEngine)) {
			// Return decision with information from outcome of each control
			return createDecision(RECOMMENDED, APPROVAL, PREFIX_APPROVAL.formatted(concatDescriptions(toDetails(resultFromRuleEngine))));
		}
		// Return decision with information from outcome of each negative control
		return createDecision(RECOMMENDED, REJECTION, PREFIX_REJECT.formatted(concatDescriptions(toDetails(resultFromRuleEngine))));
	}

	private static Decision createDecision(DecisionTypeEnum decisionType, DecisionOutcomeEnum decisionOutcomeEnum, String description) {
		return new Decision()
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcomeEnum)
			.description(description)
			.created(toOffsetDateTimeWithLocalOffset(OffsetDateTime.now(ZoneId.systemDefault())));
	}

	private static String concatDescriptions(List<ResultDetail> detailsFromRuleEngine) {
		final String transformed = transformToString(ofNullable(detailsFromRuleEngine).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(ResultDetail::getDescription)
			.toList());

		if (isBlank(transformed)) {
			return null;
		}

		return transformed.concat(".");
	}

	private static String transformToString(List<String> strings) {
		final String concatenated = String.join(", ", ofNullable(strings).orElse(emptyList()));

		return capitalize(Pattern.compile(REGEXP_LAST_COMMA).matcher(concatenated).replaceAll("$1 och $3"));
	}

	private static boolean isApproved(Result resultFromRuleEngine) {
		return PASS.equals(resultFromRuleEngine.getValue());
	}

	private static List<ResultDetail> toDetails(Result resultFromRuleEngine) {
		return Optional.ofNullable(resultFromRuleEngine.getDetails()).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.toList();
	}
}
