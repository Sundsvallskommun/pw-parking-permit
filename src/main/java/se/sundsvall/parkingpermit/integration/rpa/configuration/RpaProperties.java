package se.sundsvall.parkingpermit.integration.rpa.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.rpa")
public record RpaProperties(int connectTimeout, int readTimeout, String folderId, String identityServerUrl) {
}
