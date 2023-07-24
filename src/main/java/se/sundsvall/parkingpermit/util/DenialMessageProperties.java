package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("message.denial")
public record DenialMessageProperties(
	String message,
	String filename,
	String subject,
	String htmlBody,
	String plainBody,
	String description,
	String lawHeading,
	String lawSfs,
	String lawChapter,
	String lawArticle) {
}
