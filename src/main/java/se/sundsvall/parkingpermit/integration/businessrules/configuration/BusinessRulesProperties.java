package se.sundsvall.parkingpermit.integration.businessrules.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.businessrules")
public record BusinessRulesProperties(int connectTimeout, int readTimeout) {}
