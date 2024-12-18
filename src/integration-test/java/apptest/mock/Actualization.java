package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPutStatus;
import static apptest.mock.api.Citizen.mockGetCitizen;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

public class Actualization {

    public static String mockActualization(String caseId, String scenarioName) {
        var scenarioAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, "check_appeal_check-appeal-task-worker---api-casedata-get-errand");
        var scenarioAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, scenarioAfterUpdatePhase, "2281");
        var scenarioAfterVerifyAdministrator = mockActualizationVerifyAdministratorStakeholder(caseId, scenarioName, scenarioAfterVerifyResident);
        var scenarioAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, scenarioAfterVerifyAdministrator);
        var scenarioAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, scenarioAfterUpdateDisplayPhase);
        return mockActualizationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus);
    }

    public static String mockActualizationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "actualization_update-phase-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "displayPhaseParameter", "Aktualisering"));


        return mockCaseDataPatch(caseId, scenarioName, state,
                "actualization_update-phase-task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Aktualisering", "UNKNOWN", "ONGOING", "Registrerad")));
    }

    public static String mockActualizationVerifyResident(String caseId, String scenarioName, String requiredScenarioState, String municipalityId) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "verify-resident-of-municipality-task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Registrerad"));


        return mockGetCitizen(scenarioName, state,
                "verify-resident-of-municipality-task-worker---api-citizen-getcitizen",
                Map.of("municipalityId", municipalityId,
                        "personId", "6b8928bb-9800-4d52-a9fa-20d88c81f1d6"));

    }

    public static String mockActualizationVerifyAdministratorStakeholder(String caseId, String scenarioName, String requiredScenarioState) {
        return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "actualization_verify-administrator-stakeholder---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Registrerad"));
    }

    public static String mockActualizationUpdateDisplayPhase(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "actualization_update-display-phase---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Registrerad"));


        return mockCaseDataPatch(caseId, scenarioName, state,
                "actualization_update-display-phase---api-casedata-patch-errand",
                equalToJson(createPatchBody("Aktualisering","UNKNOWN", "ONGOING", "Granskning")));
    }

    public static String mockActualizationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "actualization_update-errand-status---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "UNKNOWN",
                        "displayPhaseParameter", "Granskning"));

        return mockCaseDataPutStatus(caseId, scenarioName, state,
                "actualization_update-errand-status---api-casedata-put-status",
                equalToJson("""
                        [
                          {
                            "statusType": "Under granskning",
                            "description": "Under granskning",
                            "dateTime": "${json-unit.any-string}"
                          }
                        ]
                        """));
    }

    public static String mockActualizationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
        var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
                "actualization_check-phase-action_task-worker---api-casedata-get-errand",
                Map.of("decisionTypeParameter", "FINAL",
                        "phaseParameter", "Aktualisering",
                        "phaseStatusParameter", "ONGOING",
                        "phaseActionParameter", "COMPLETE",
                        "displayPhaseParameter", "Granskning"));

        return mockCaseDataPatch(caseId, scenarioName, state,
                "actualization_check-phase-action_task-worker---api-casedata-patch-errand",
                equalToJson(createPatchBody("Aktualisering", "COMPLETE", "COMPLETED", "Granskning")));
    }
}