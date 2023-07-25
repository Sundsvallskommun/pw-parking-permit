package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("texts.denial")
public record DenialTextProperties(
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
