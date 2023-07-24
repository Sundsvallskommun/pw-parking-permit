package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

@Component
@ExternalTaskSubscription("InvestigationDummyTask")
public class InvestigationDummyTaskWorker extends AbstractTaskWorker {
	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for InvestigationDummyTask");

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
