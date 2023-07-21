package se.sundsvall.parkingpermit.integration.messaging.mapper;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mappers.messaging")
public record MessagingMapperProperties(
	String message,
	String filename,
	String department,
	String subject,
	String htmlBody,
	String plainBody,
	String contactInfoEmail,
	String contactInfoPhonenumber,
	String contactInfoText,
	String contactInfoUrl,
	String approvalDescription,
	String dismissalDescription,
	String lawHeading,
	String lawSfs,
	String lawChapter,
	String lawArticle) {
}
