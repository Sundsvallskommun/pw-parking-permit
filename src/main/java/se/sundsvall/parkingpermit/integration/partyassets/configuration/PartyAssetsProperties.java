package se.sundsvall.parkingpermit.integration.partyassets.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.partyassets")
public record PartyAssetsProperties(int connectTimeout, int readTimeout) {}
