package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static wiremock.org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static wiremock.org.eclipse.jetty.http.HttpStatus.OK_200;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

public class PartyAssets {

	public static String mockPartyAssetsPost(String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo("/api-party-assets/2281/assets"))
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

	public static String mockPartyAssetsGet(String scenarioName, String requiredScenarioState, String newScenarioState, String assetId, String partyId, String status) {
		return stubFor(get(urlEqualTo(String.format("/api-party-assets/2281/assets?assetId=%s&partyId=%s&status=%s", assetId, partyId, status)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(OK_200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/partyassets/get-assets.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockPartyAssetsGetByPartyIdAndStatus(String scenarioName, String requiredScenarioState, String newScenarioState, String partyId, String status) {
		return stubFor(get(urlEqualTo(String.format("/api-party-assets/2281/assets?partyId=%s&status=%s", partyId, status)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(OK_200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/partyassets/get-assets.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockPartyAssetsPut(String id, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(put(urlEqualTo(String.format("/api-party-assets/2281/assets/%s", id)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(NO_CONTENT_204)
				.withHeader("Content-Type", "*/*"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}
}
