package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class PartyAssets {

    public static String mockPartyAssetsPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
        return stubFor(post(urlEqualTo("/api-party-assets/2281/assets"))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs(requiredScenarioState)
                        .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                        .withRequestBody(bodyPattern)
                        .willReturn(aResponse()
                                .withStatus(201)
                                .withHeader("Content-Type", "*/*"))
                        .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }
}
