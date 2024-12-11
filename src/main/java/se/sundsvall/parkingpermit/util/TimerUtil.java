package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Decision;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;

import static java.util.Objects.nonNull;

public final class TimerUtil {

	private TimerUtil() {}

	public static Date getControlMessageTime(Decision decision, String controlMessageDelay) {
		var decisionCreated = OffsetDateTime.now();
		if(nonNull(decision) && nonNull(decision.getCreated())) {
			decisionCreated = decision.getCreated();
		}
		final var duration = Duration.parse(controlMessageDelay);
		return Date.from(decisionCreated.plus(duration).toInstant());
	}
}
