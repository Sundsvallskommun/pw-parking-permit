package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static wiremock.org.eclipse.jetty.http.HttpStatus.CREATED_201;

import com.github.tomakehurst.wiremock.matching.ContentPattern;
import java.util.UUID;

public class SupportManagement {

	public static String mockSupportManagementPost(final String scenarioName, final String requiredScenarioState, final String newScenarioState, final ContentPattern<?> bodyPattern) {
		final var smErrandId = UUID.randomUUID().toString();
		return stubFor(post("/api-support-management/2260/SBK_PARKING_PERMIT/errands")
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(CREATED_201)
				.withHeader("Content-Type", "application/json")
				.withHeader("Location", String.format("/api-support-management/2260/SBK_PARKING_PERMIT/errands/%s", smErrandId)))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}
}
