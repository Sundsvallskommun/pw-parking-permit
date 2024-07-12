package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_ONGOING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import java.util.HashMap;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("UpdateErrandPhaseTask")
public class UpdateErrandPhaseTaskWorker extends AbstractTaskWorker {

	UpdateErrandPhaseTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
			final String phase = externalTask.getVariable(CAMUNDA_VARIABLE_PHASE);
			final String displayPhase = externalTask.getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE);

			final var errand = getErrand(municipalityId, caseNumber);
			logInfo("Executing update of phase for errand with id {}", errand.getId());

			ofNullable(phase).ifPresentOrElse(
				phaseValue -> {
					final var newDisplayPhase = ofNullable(displayPhase).orElse(phaseValue);
					logInfo("Setting phase to {}", phaseValue);
					// Set phase action to unknown to errand in the beginning of the phase
					caseDataClient.patchErrand(municipalityId, errand.getId(), toPatchErrand(errand.getExternalCaseId(), phaseValue, PHASE_STATUS_ONGOING, PHASE_ACTION_UNKNOWN, newDisplayPhase));
				},
				() -> logInfo("Phase is not set"));

			// Set phase action to unknown in the beginning of the phase
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_UNKNOWN);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
