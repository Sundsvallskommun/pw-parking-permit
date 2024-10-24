package se.sundsvall.parkingpermit.util;

import java.time.OffsetDateTime;
import java.util.Date;

public final class TimerUtil {

	private TimerUtil() {}

	public static Date getControlMessageTime(int controlMessageDelay) {
		return Date.from(OffsetDateTime.now().plusDays(controlMessageDelay).toInstant());
	}
}
