package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class BusinessRules {

    public static String mockBusinessRulesPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern, Map<String, Object> transformParameters) {
        return stubFor(post(urlEqualTo(String.format("/api-business-rules/2281/engine")))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .withRequestBody(bodyPattern)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("common/responses/businessrules/rules-result.json")
                        .withTransformers("response-template")
                        .withTransformerParameters(transformParameters))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }
}
