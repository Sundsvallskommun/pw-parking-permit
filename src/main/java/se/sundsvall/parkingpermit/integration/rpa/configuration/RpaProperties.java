package se.sundsvall.parkingpermit.integration.rpa.configuration;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.rpa")
public record RpaProperties(int connectTimeout, int readTimeout, Map<String, String> folderIds, String identityServerUrl) {
}
