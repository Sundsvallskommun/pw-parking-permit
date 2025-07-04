package se.sundsvall.parkingpermit.integration.supportmanagement.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.support-management")
public record SupportManagementProperties(int connectTimeout, int readTimeout) {
}
