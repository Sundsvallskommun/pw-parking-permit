package apptest.mock.api;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.OK_200;

public class Citizen {

    public static String mockGetCitizen(String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters) {
        return stubFor(get(urlEqualTo(String.format("/api-citizen/%s", transformParameters.get("personId"))))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("common/responses/citizen/get-citizen.json")
                        .withTransformers("response-template")
                        .withTransformerParameters(transformParameters))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }
}
