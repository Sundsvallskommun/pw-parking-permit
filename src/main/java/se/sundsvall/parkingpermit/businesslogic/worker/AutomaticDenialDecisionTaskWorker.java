package se.sundsvall.parkingpermit.businesslogic.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("AutomaticDenialDecisionTask")
public class AutomaticDenialDecisionTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticDenialDecisionTaskWorker.class);

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			LOGGER.info("Execute Worker for AutomaticDenialDecisionTask");

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in execution for task with id {} and businesskey {}", externalTask.getId(), externalTask.getBusinessKey());

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
