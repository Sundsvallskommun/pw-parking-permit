package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("texts.approval")
public record ApprovalTextProperties(String description) {}
