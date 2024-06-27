package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_DISPLAY_PHASE;
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
			final var errand = getErrand(externalTask);
			logInfo("Executing update of phase for errand with id {}", errand.getId());

			final var phase = externalTask.getVariable(CAMUNDA_VARIABLE_PHASE);

			ofNullable(phase).ifPresentOrElse(
				phaseValue -> {
					final var displayPhase = ofNullable(externalTask.getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE)).orElse(phaseValue).toString();
					logInfo("Setting phase to {}", phaseValue);
					// Set phase action to unknown to errand in the beginning of the phase
					caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), phaseValue.toString(), PHASE_STATUS_ONGOING, PHASE_ACTION_UNKNOWN, displayPhase));
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
