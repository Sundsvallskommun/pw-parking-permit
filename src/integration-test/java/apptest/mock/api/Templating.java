package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Templating {

	public static String mockRenderPdf(final String scenarioName, final String requiredScenarioState, final String newScenarioState, final ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo("/api-templating/2281/render/pdf"))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withRequestBody(bodyPattern)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/templating/render-pdf.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}
}
