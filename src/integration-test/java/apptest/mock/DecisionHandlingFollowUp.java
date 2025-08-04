package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataNotesDelete;
import static apptest.mock.api.CaseData.mockCaseDataNotesGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class DecisionHandlingFollowUp {

	public static String mockFollowUp(final String municipalityId, final String caseId, final String scenarioName, final boolean isAutomatic) {
		final var stateAfterUpdatePhase = mockFollowUpUpdatePhaseAtStart(municipalityId, caseId, scenarioName, "execution_send-simplified-service-task-worker---api-casedata-get-errand", isAutomatic);
		final var stateAfterCheckPhaseAction = mockFollowUpCheckPhaseAction(municipalityId, caseId, scenarioName, stateAfterUpdatePhase, isAutomatic);
		final var stateAfterCleanUp = mockFollowUpCleanUpNotes(municipalityId, caseId, scenarioName, stateAfterCheckPhaseAction);
		final var stateAfterUpdateStatus = mockFollowUpUpdateStatus(municipalityId, caseId, scenarioName, stateAfterCleanUp, isAutomatic);
		return mockFollowUpUpdatePhaseAtEnd(municipalityId, caseId, scenarioName, stateAfterUpdateStatus, isAutomatic);
	}

	public static String mockFollowUpCheckPhaseAction(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {
		var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"follow_up_check-phase-action_task-worker---api-casedata-get-errand-check-phase-action",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Uppföljning",
				"phaseStatusParameter", "ONGOING",
				"statusTypeParameter", "Status",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Uppföljning"));

		return mockCaseDataPatch(municipalityId, caseId, scenarioName, state,
			"follow_up_check-phase-action_task-worker---api-casedata-patch-errand-start",
			equalToJson(createPatchBody("Uppföljning", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_COMPLETE, "COMPLETED", "Uppföljning")));
	}

	public static String mockFollowUpUpdatePhaseAtStart(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {

		var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"follow_up_update-phase-task-worker---api-casedata-get-errand-update-phase-start",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"statusTypeParameter", "Status",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatch(municipalityId, caseId, scenarioName, state,
			"follow_up_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Uppföljning", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "ONGOING", "Uppföljning")));
	}

	public static String mockFollowUpUpdatePhaseAtEnd(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {

		var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"follow_up_update-phase-task-worker---api-casedata-get-errand-update-phase-end",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Uppföljning",
				"phaseStatusParameter", "COMPLETED",
				"statusTypeParameter", "Ärende avslutat",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Uppföljning"));

		return mockCaseDataPatch(municipalityId, caseId, scenarioName, state,
			"follow_up_update-phase-task-worker---api-casedata-patch-errand-end",
			equalToJson(createPatchBody("Uppföljning", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN, "COMPLETED", "Uppföljning")));
	}

	public static String mockFollowUpCleanUpNotes(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState) {
		var state = mockCaseDataNotesGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"follow_up_update-phase-task-worker---api-casedata-get-notes", "INTERNAL");

		return mockCaseDataNotesDelete(municipalityId, caseId, "128", scenarioName, state,
			"follow_up_update-phase-task-worker---api-casedata-delete-note");

	}

	public static String mockFollowUpUpdateStatus(final String municipalityId, final String caseId, final String scenarioName, final String requiredScenarioState, final boolean isAutomatic) {
		var state = mockCaseDataGet(municipalityId, caseId, scenarioName, requiredScenarioState,
			"follow_up_update-phase-task-worker---api-casedata-get-errand-update-status",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Uppföljning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", isAutomatic ? PHASE_ACTION_AUTOMATIC : PHASE_ACTION_UNKNOWN,
				"statusTypeParameter", "Status",
				"displayPhaseParameter", "Uppföljning"));

		return mockCaseDataPatchStatus(municipalityId, caseId, scenarioName, state,
			"follow_up_update-errand-status---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Ärende avslutat",
				    "description": "Ärende avslutat",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}
}
