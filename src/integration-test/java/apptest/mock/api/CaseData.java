package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static wiremock.org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static wiremock.org.eclipse.jetty.http.HttpStatus.OK_200;

public class CaseData {

	public static String mockCaseDataGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioState, transformParameters, "APPROVAL", "ADMINISTRATOR");
	}

	public static String mockCaseDataGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters, String decisionOutcome) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioState, transformParameters, decisionOutcome, "ADMINISTRATOR");
	}

	public static String mockCaseDataGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters, String decisionOutcome, String role) {
		return stubFor(get(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s", caseId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(OK_200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/casedata/get-errand.json")
				.withTransformers("response-template")
				.withTransformerParameter("caseId", caseId)
				.withTransformerParameter("decisionOutcome", decisionOutcome)
				.withTransformerParameter("role", role)
				.withTransformerParameters(transformParameters))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockCaseDataStakeholdersGet(String caseId, String stakeholderId, String scenarioName, String requiredScenarioState, String newScenarioState) {
		return stubFor(get(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/stakeholders/%s", caseId, stakeholderId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(OK_200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/casedata/get-stakeholder.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockCaseDataPatch(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(patch(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s", caseId)))
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

	public static String mockCaseDataDecisionPatch(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(patch(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/decisions", caseId)))
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

	public static String mockCaseDataAddStakeholderPatch(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(patch(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/stakeholders", caseId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withRequestBody(bodyPattern)
			.willReturn(aResponse()
				.withStatus(NO_CONTENT_204)
				.withHeader("Content-Type", "*/*")
				.withHeader("Location", String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/stakeholders/", caseId) + "2"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockCaseDataPutStatus(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(put(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/statuses", caseId)))
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

	public static String mockCaseDataNotesGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, String noteType) {
		return stubFor(get(urlPathEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/notes", caseId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.withQueryParam("noteType", equalTo(noteType))
			.willReturn(aResponse()
				.withStatus(OK_200)
				.withHeader("Content-Type", "application/json")
				.withBodyFile("common/responses/casedata/get-notes.json"))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockCaseDataNotesDelete(String caseId, String noteId, String scenarioName, String requiredScenarioState, String newScenarioState) {
		return stubFor(delete(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/notes/%s", caseId, noteId)))
			.inScenario(scenarioName)
			.whenScenarioStateIs(requiredScenarioState)
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(NO_CONTENT_204))
			.willSetStateTo(newScenarioState))
			.getNewScenarioState();
	}

	public static String mockCaseDataAddMessagePost(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
		return stubFor(post(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKING_PERMIT/errands/%s/messages", caseId)))
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

	public static String createPatchBody(String phase, String phaseAction, String phaseStatus, String displayPhase) {
		return String.format("""
			{
				"externalCaseId" : "2971",
				"phase" : "%s",
				"extraParameters" : [ {
						"key" : "disability.walkingAbility",
						"values" : [ "false" ]
					}, {
						"key" : "application.applicant.testimonial",
						"values" : [ "true" ]
					}, {
						"key" : "consent.view.transportationServiceDetails",
						"values" : [ "false" ]
					}, {
						"key" : "disability.aid",
						"values" : [ "Inget" ]
					}, {
						"key" : "disability.canBeAloneWhileParking",
						"values" : [ "true" ]
					}, {
						"key" : "application.role",
						"values" : [ "SELF" ]
					}, {
						"key" : "application.applicant.capacity",
						"values" : [ "DRIVER" ]
					}, {
						"key" : "application.applicant.signingAbility",
						"values" : [ "false" ]
					}, {
						"key" : "disability.walkingDistance.max",
						"values" : [ ]
					}, {
						"key" : "disability.walkingDistance.beforeRest",
						"values" : [ ]
					}, {
						"key" : "consent.contact.doctor",
						"values" : [ "false" ]
					}, {
						"key" : "application.reason",
						"values" : [ "" ]
					}, {
						"key" : "disability.canBeAloneWhileParking.note",
						"values" : [ ]
					}, {
						"key" : "disability.duration",
						"values" : [ "P6M" ]
					}, {
						"key" : "artefact.permit.number",
						"values" : [ "" ]
					}, {
						"key" : "process.phaseStatus",
						"values" : [ "%s" ]
					}, {
						"key" : "process.phaseAction",
						"values" : [ "%s" ]
					}, {
						"key" : "process.displayPhase",
						"values" : [ "%s" ]
					} ]
					}""", phase, phaseStatus, phaseAction, displayPhase);
	}
}