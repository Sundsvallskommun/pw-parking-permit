package apptest.mock.api;

import com.github.tomakehurst.wiremock.matching.ContentPattern;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class CaseData {

    public static String mockCaseDataGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters) {
        return mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioState, transformParameters, "APPROVAL");
    }

    public static String mockCaseDataGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, Map<String, Object> transformParameters, String decisionOutcome) {
        return stubFor(get(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s", caseId)))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("common/responses/casedata/get-errand.json")
                        .withTransformers("response-template")
                        .withTransformerParameter("caseId", caseId)
                        .withTransformerParameter("decisionOutcome", decisionOutcome)
                        .withTransformerParameters(transformParameters))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }

    public static String mockCaseDataPatch(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
        return stubFor(patch(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s", caseId)))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs(requiredScenarioState)
                        .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                        .withRequestBody(bodyPattern)
                        .willReturn(aResponse()
                                .withStatus(204)
                                .withHeader("Content-Type", "*/*"))
                        .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }

    public static String mockCaseDataDecisionPatch(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
        return stubFor(patch(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s/decisions", caseId)))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .withRequestBody(bodyPattern)
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "*/*"))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }

    public static String mockCaseDataPutStatus(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, ContentPattern<?> bodyPattern) {
        return stubFor(put(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s/statuses", caseId)))
                .inScenario(scenarioName)
                .whenScenarioStateIs(requiredScenarioState)
                .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
                .withRequestBody(bodyPattern)
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "*/*"))
                .willSetStateTo(newScenarioState))
                .getNewScenarioState();
    }

    public static String mockCaseDataNotesGet(String caseId, String scenarioName, String requiredScenarioState, String newScenarioState, String noteType) {
        return stubFor(get(urlPathEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s/notes", caseId)))
            .inScenario(scenarioName)
            .whenScenarioStateIs(requiredScenarioState)
            .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
            .withQueryParam("noteType", equalTo(noteType))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("common/responses/casedata/get-notes.json"))
            .willSetStateTo(newScenarioState))
            .getNewScenarioState();
    }

    public static String mockCaseDataNotesDelete(String caseId, String noteId, String scenarioName, String requiredScenarioState, String newScenarioState) {
        return stubFor(delete(urlEqualTo(String.format("/api-casedata/2281/SBK_PARKINGPERMIT/errands/%s/notes/%s", caseId, noteId)))
            .inScenario(scenarioName)
            .whenScenarioStateIs(requiredScenarioState)
            .withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
            .willReturn(aResponse()
                .withStatus(204))
            .willSetStateTo(newScenarioState))
            .getNewScenarioState();
    }
}
