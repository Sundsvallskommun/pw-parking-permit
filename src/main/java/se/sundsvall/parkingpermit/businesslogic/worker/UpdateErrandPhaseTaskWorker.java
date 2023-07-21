package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.PHASE_DECISION;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("UpdateErrandPhaseTask")
public class UpdateErrandPhaseTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateErrandPhaseTaskWorker.class);

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			LOGGER.info("Executing update of phase for errand with id {}", errand.getId());

			caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), PHASE_DECISION));
			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
