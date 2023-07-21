package se.sundsvall.parkingpermit.integration.templating.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.templating")
public record TemplatingProperties(int connectTimeout, int readTimeout) {
}
