package se.sundsvall.parkingpermit.businesslogic.util;

import static generated.se.sundsvall.businessrules.ResultValue.PASS;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.RECOMMENDED;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.dept44.util.DateUtils.toOffsetDateTimeWithLocalOffset;

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

public final class BusinessRulesUtil {

	static final String RECOMMENDED_PREFIX_APPROVAL = "Rekommenderat beslut är bevilja. %s";
	static final String RECOMMENDED_PREFIX_REJECT = "Rekommenderat beslut är avslag. %s";
	static final String AUTOMATIC_PREFIX_APPROVAL = "Beslut är bevilja. %s";
	static final String AUTOMATIC_PREFIX_REJECT = "Beslut är avslag. %s";

	private static final String REGEXP_LAST_COMMA = "^(.*)(, )(.*)$";

	private BusinessRulesUtil() {}

	public static Decision constructDecision(Result resultFromRuleEngine, boolean isAutomatic) {
		var isApproved = isApproved(resultFromRuleEngine);

		var decisionType = isAutomatic ? FINAL : RECOMMENDED;
		var decisionOutcome = isApproved ? APPROVAL : REJECTION;
		String prefix = getDescriptionPrefix(isAutomatic, isApproved);

		return createDecision(decisionType, decisionOutcome, prefix.formatted(concatDescriptions(toDetails(resultFromRuleEngine))), isAutomatic);
	}

	private static String getDescriptionPrefix(boolean isAutomatic, boolean isApproved) {
		if (isAutomatic) {
			return isApproved ? AUTOMATIC_PREFIX_APPROVAL : AUTOMATIC_PREFIX_REJECT;
		} else {
			return isApproved ? RECOMMENDED_PREFIX_APPROVAL : RECOMMENDED_PREFIX_REJECT;
		}
	}

	private static Decision createDecision(DecisionTypeEnum decisionType, DecisionOutcomeEnum decisionOutcomeEnum, String description, boolean isAutomatic) {
		final var decisison = new Decision()
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcomeEnum)
			.description(description)
			.created(toOffsetDateTimeWithLocalOffset(OffsetDateTime.now(ZoneId.systemDefault())));

		if (FINAL.equals(decisionType) && isAutomatic) {
			decisison.setDecidedAt(OffsetDateTime.now());
		}
		return decisison;
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
