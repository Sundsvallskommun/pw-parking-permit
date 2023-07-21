package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

import java.util.List;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("UpdateErrandStatusTask")
public class UpdateErrandStatusTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateErrandStatusTaskWorker.class);

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			LOGGER.info("Executing update of status for errand with id {}", errand.getId());

			caseDataClient.putStatus(errand.getId(), List.of(toStatus(STATUS_DECISION_EXECUTED, "Ärendet avvisas")));
			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
