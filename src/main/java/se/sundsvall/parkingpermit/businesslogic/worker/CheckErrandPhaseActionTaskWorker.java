package se.sundsvall.parkingpermit.businesslogic.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

@Component
@ExternalTaskSubscription("CheckErrandPhaseActionTask")
public class CheckErrandPhaseActionTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			clearUpdateAvailable(externalTask);

			final var errand = getErrand(externalTask);
			logInfo("Check phase action for errand with id {}", errand.getId());

			final var phaseAction = ofNullable(errand.getExtraParameters())
				.map(extraParameters -> extraParameters.get(CASEDATA_KEY_PHASE_ACTION))
				.orElse(PHASE_ACTION_UNKNOWN);

			switch (phaseAction) {
				case PHASE_ACTION_COMPLETE -> {
					logInfo("Phase action is complete. Setting phase status to {}", PHASE_STATUS_COMPLETED);
					caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_COMPLETED, phaseAction));
				}
				case PHASE_ACTION_CANCEL -> {
					logInfo("Phase action is cancel. Setting phase status to {}", PHASE_STATUS_CANCELED);
					caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_CANCELED, phaseAction));
				}
				default -> {
					logInfo("Phase action is unknown. Setting phase status to {}", PHASE_STATUS_WAITING);
					caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_WAITING, phaseAction));
				}
			}

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, phaseAction);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
