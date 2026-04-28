package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.CREATED_201;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

public class Relations {

	public static String mockRelationsPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo("/api-relation/2281/relations"))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(CREATED_201)
				.withHeader("Content-Type", "*/*"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}
}
