package apptest.mock.api;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Citizen {

    public static String mockGetCitizen(String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters) {
        return stubFor(get(urlEqualTo(String.format("/api-citizen/%s", transformParameters.get("personId"))))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("common/responses/citizen/get-citizen.json")
                        .withTransformers("response-template")
                        .withTransformerParameters(transformParameters))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }
}
