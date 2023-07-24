package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("message.common")
public record CommonMessageProperties(
	String department,
	String contactInfoEmail,
	String contactInfoPhonenumber,
	String contactInfoText,
	String contactInfoUrl) {
}
