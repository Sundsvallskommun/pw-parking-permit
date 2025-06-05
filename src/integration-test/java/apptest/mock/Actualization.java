package apptest.mock;

import java.util.Map;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Citizen.mockGetCitizen;
import static apptest.mock.api.Citizen.mockGetCitizenNoContent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

public class Actualization {

	private static final String MUNICIPALITY_ID = "2281";

	public static String mockActualization(String caseId, String scenarioName, boolean isAutomatic) {
		var scenarioAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, "check_appeal_check-appeal-task-worker---api-casedata-get-errand", isAutomatic);
		var scenarioAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, scenarioAfterUpdatePhase, "2281", isAutomatic);
		var scenarioAfterVerifyAdministrator = mockActualizationVerifyAdministratorStakeholder(caseId, scenarioName, scenarioAfterVerifyResident, isAutomatic);
		var scenarioAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, scenarioAfterVerifyAdministrator, isAutomatic);
		var scenarioAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, scenarioAfterUpdateDisplayPhase, isAutomatic);
		return mockActualizationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus, isAutomatic);
	}

	public static String mockActualizationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"displayPhaseParameter", "Aktualisering",
				"phaseActionParameter", phaseAction(isAutomatic)));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", phaseAction(isAutomatic), "ONGOING", "Registrerad")));
	}

	public static String mockActualizationVerifyResident(String caseId, String scenarioName, String requiredScenarioState, String municipalityId, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"verify-resident-of-municipality-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", phaseAction(isAutomatic),
				"displayPhaseParameter", "Registrerad"));

		if (!MUNICIPALITY_ID.equals(municipalityId)) {
			return mockGetCitizenNoContent(scenarioName, state,
				"verify-resident-of-municipality-task-worker---api-citizen-getcitizen",
				"6b8928bb-9800-4d52-a9fa-20d88c81f1d6");
		}

		return mockGetCitizen(scenarioName, state,
			"verify-resident-of-municipality-task-worker---api-citizen-getcitizen",
			Map.of("municipalityId", municipalityId,
				"personId", "6b8928bb-9800-4d52-a9fa-20d88c81f1d6"));

	}

	public static String mockActualizationVerifyAdministratorStakeholder(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_verify-administrator-stakeholder---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", phaseAction(isAutomatic),
				"displayPhaseParameter", "Registrerad"));
	}

	public static String mockActualizationUpdateDisplayPhase(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-display-phase---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", phaseAction(isAutomatic),
				"displayPhaseParameter", "Registrerad"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_update-display-phase---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", phaseAction(isAutomatic), "ONGOING", "Granskning")));
	}

	public static String mockActualizationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-errand-status---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", phaseAction(isAutomatic),
				"displayPhaseParameter", "Granskning"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"actualization_update-errand-status---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under granskning",
				    "description": "Under granskning",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockActualizationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState, boolean isAutomatic) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Granskning"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_check-phase-action_task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE, "COMPLETED", "Granskning")));
	}

	private static String phaseAction(boolean isAutomatic) {
		return isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN;
	}
}
