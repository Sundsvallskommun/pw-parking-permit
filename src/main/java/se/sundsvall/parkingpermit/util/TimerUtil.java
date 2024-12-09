package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Decision;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;

public final class TimerUtil {

	private TimerUtil() {}

	public static Date getControlMessageTime(Decision decision, String controlMessageDelay) {
		final var decisionCreated = Optional.ofNullable(decision.getCreated()).orElse(OffsetDateTime.now());
		final var duration = Duration.parse(controlMessageDelay);
		return Date.from(decisionCreated.plus(duration).toInstant());
	}
}
