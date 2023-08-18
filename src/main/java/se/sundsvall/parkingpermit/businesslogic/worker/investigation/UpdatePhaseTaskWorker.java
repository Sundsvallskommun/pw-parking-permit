package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

@Component
@ExternalTaskSubscription("InvestigationUpdatePhaseTask")
public class UpdatePhaseTaskWorker extends AbstractTaskWorker {
	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for UpdatePhaseTaskWorker");

			// TODO: Update case in caseData with new phase

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
