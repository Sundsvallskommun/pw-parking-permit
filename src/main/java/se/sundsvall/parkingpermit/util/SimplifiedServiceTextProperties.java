package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("texts.simplified-service")
public record SimplifiedServiceTextProperties(
	String message,
	String subject,
	String htmlBody,
	String plainBody,
	String description,
	String delay) {}
