package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import java.util.Map;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.worker.AbstractWorker;

@Component
@ExternalTaskSubscription("ActualizationDummyTask")
public class ActualizationDummyTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActualizationDummyTaskWorker.class);

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			LOGGER.info("Execute Worker for ActualizationDummyTask");

			externalTaskService.complete(externalTask, Map.of("isResidentOfMunicipality", "citizen".equalsIgnoreCase(externalTask.getBusinessKey())));
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in execution for task with id {} and businesskey {}", externalTask.getId(), externalTask.getBusinessKey());

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
