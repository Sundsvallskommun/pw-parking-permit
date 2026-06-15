package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Decision;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
public class TimerUtil {

	private final Clock clock;

	public TimerUtil() {
		this(Clock.systemDefaultZone());
	}

	TimerUtil(Clock clock) {
		this.clock = clock;
	}

	public Date getControlMessageTime(Decision decision, String controlMessageDelay) {
		var decisionCreated = OffsetDateTime.now(clock);
		if (nonNull(decision) && nonNull(decision.getCreated())) {
			decisionCreated = decision.getCreated();
		}
		final var duration = Duration.parse(controlMessageDelay);
		return Date.from(decisionCreated.plus(duration).toInstant());
	}
}
