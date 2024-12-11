package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Decision;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

class TimerUtilTest {

	@Test
	void getTimeWithTenDaysDelay() {
		final var decision = new Decision().created(OffsetDateTime.now()).decisionType(Decision.DecisionTypeEnum.FINAL);
		final var result = TimerUtil.getControlMessageTime(decision, "P10D");

		assertThat(result).isCloseTo(now().plusDays(10).toInstant(), 2000);
	}

	@Test
	void getTimeWithZeroDaysDelay() {
		final var decision = new Decision().created(OffsetDateTime.now()).decisionType(Decision.DecisionTypeEnum.FINAL);
		final var result = TimerUtil.getControlMessageTime(decision, "PT0S");

		assertThat(result).isCloseTo(now().toInstant(), 2000);
	}

	@Test
	void getTimeWithNegativeDaysDelay() {
		final var decision = new Decision().created(OffsetDateTime.now()).decisionType(Decision.DecisionTypeEnum.FINAL);
		final var result = TimerUtil.getControlMessageTime(decision, "-P10D");

		assertThat(result).isCloseTo(now().minusDays(10).toInstant(), 2000);
	}
}
