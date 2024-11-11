package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.OK_200;

public class BusinessRules {

    public static String mockBusinessRulesPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern, Map<String, Object> transformParameters) {
        return mockBusinessRulesPost(scenarioName, requiredScenarioState, newScenarioState, bodyPattern, transformParameters, true);
    }

    public static String mockBusinessRulesPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern, Map<String, Object> transformParameters, boolean validResponse) {
        var bodyFile = validResponse ? "common/responses/businessrules/rules-result.json" : "common/responses/businessrules/rules-result-validation-error.json";

        return stubFor(post(urlEqualTo("/api-business-rules/2281/engine"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .withRequestBody(bodyPattern)
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(bodyFile)
                        .withTransformers("response-template")
                        .withTransformerParameters(transformParameters))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }
}
