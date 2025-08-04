package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

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

	public static void mockMessagingWebMessagePost(final String municipalityId, final ContentPattern<?> bodyPattern) {
		stubFor(post(urlEqualTo(String.format("/api-messaging/%s/webmessage", municipalityId)))
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/messaging/web-message.json")));
	}

	public static String mockMessagingDigitalMailPost(final String municipalityId, final String scenarioName, final String requiredScenarioState, final String newScenarioState, final ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo(String.format("/api-messaging/%s/digital-mail", municipalityId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(201)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/messaging/digital-mail.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}
}
