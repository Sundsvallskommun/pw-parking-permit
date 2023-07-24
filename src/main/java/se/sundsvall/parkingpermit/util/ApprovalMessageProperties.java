package se.sundsvall.parkingpermit.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("message.approval")
public record ApprovalMessageProperties(String description) {
}
