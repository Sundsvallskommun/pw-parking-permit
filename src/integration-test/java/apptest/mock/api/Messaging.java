package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Messaging {

	public static String mockMessagingWebMessagePost(final String scenarioName, final String requiredScenarioState, final String newScenarioState, final ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo("/api-messaging/2281/webmessage"))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/messaging/web-message.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static void mockMessagingWebMessagePost(final ContentPattern<?> bodyPattern) {
		stubFor(post(urlEqualTo("/api-messaging/2281/webmessage"))
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/messaging/web-message.json")));
	}
}
