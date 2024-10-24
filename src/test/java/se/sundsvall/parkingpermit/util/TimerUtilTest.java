package se.sundsvall.parkingpermit.util;

import org.junit.jupiter.api.Test;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

class TimerUtilTest {

	@Test
	void getTimeWithTenDaysDelay() {
		final var result = TimerUtil.getControlMessageTime(10);

		assertThat(result).isCloseTo(now().plusDays(10).toInstant(), 2000);
	}

	@Test
	void getTimeWithZeroDaysDelay() {
		final var result = TimerUtil.getControlMessageTime(0);

		assertThat(result).isCloseTo(now().toInstant(), 2000);
	}

	@Test
	void getTimeWithNegativeDaysDelay() {
		final var result = TimerUtil.getControlMessageTime(-10);

		assertThat(result).isCloseTo(now().minusDays(10).toInstant(), 2000);
	}
}
