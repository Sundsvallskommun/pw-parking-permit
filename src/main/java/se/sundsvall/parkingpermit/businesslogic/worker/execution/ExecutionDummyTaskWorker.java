package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

@Component
@ExternalTaskSubscription("ExecutionDummyTask")
public class ExecutionDummyTaskWorker extends AbstractTaskWorker {
	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ExecutionDummyTask");

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
