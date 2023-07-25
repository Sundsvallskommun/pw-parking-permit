package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("texts.common")
public record CommonTextProperties(
	String department,
	String contactInfoEmail,
	String contactInfoPhonenumber,
	String contactInfoText,
	String contactInfoUrl) {
}
