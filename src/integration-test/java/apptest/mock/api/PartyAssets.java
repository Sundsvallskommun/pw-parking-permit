package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static wiremock.org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static wiremock.org.eclipse.jetty.http.HttpStatus.OK_200;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

public class PartyAssets {

	public static String mockPartyAssetsPost(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(post(urlPathEqualTo("/api-party-assets/2281/assets"))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withQueryParam("sourceReference", equalTo(String.format("LINK|%s;case;casedata;SBK_PARKING_PERMIT|",caseId)))
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(CREATED_201)
				.withHeader("Content-Type", "*/*"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockPartyAssetsPost(String caseId, String scenarioName, String municipalityId, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(post(urlPathEqualTo(String.format("/api-party-assets/%s/assets", municipalityId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withQueryParam("sourceReference", equalTo(String.format("LINK|%s;case;casedata;SBK_PARKING_PERMIT|", caseId)))
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

	public static String mockPartyAssetsPatch(String id, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(patch(urlEqualTo(String.format("/api-party-assets/2281/assets/%s", id)))
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
