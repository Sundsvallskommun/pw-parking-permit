package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Decision;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimerUtilTest {

	private static final OffsetDateTime DECISION_CREATED = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-06-01T08:00:00Z"), ZoneOffset.UTC);

	private final TimerUtil timerUtil = new TimerUtil(FIXED_CLOCK);

	@Test
	void getTimeWithTenDaysDelay() {
		final var decision = new Decision().created(DECISION_CREATED).decisionType(Decision.DecisionTypeEnum.FINAL);

		final var result = timerUtil.getControlMessageTime(decision, "P10D");

		assertThat(result).isEqualTo(Date.from(DECISION_CREATED.plusDays(10).toInstant()));
	}

	@Test
	void getTimeWithZeroDaysDelay() {
		final var decision = new Decision().created(DECISION_CREATED).decisionType(Decision.DecisionTypeEnum.FINAL);

		final var result = timerUtil.getControlMessageTime(decision, "PT0S");

		assertThat(result).isEqualTo(Date.from(DECISION_CREATED.toInstant()));
	}

	@Test
	void getTimeWithNegativeDaysDelay() {
		final var decision = new Decision().created(DECISION_CREATED).decisionType(Decision.DecisionTypeEnum.FINAL);

		final var result = timerUtil.getControlMessageTime(decision, "-P10D");

		assertThat(result).isEqualTo(Date.from(DECISION_CREATED.minusDays(10).toInstant()));
	}

	@Test
	void getTimeFallsBackToClockWhenDecisionHasNoCreated() {
		final var result = timerUtil.getControlMessageTime(null, "P1D");

		assertThat(result).isEqualTo(Date.from(OffsetDateTime.now(FIXED_CLOCK).plusDays(1).toInstant()));
	}
}
