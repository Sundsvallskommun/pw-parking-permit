package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

@Component
@ExternalTaskSubscription("ActualizationDummyTask")
public class ActualizationDummyTaskWorker extends AbstractTaskWorker {
	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ActualizationDummyTask");

			// Cases with even caseNumber is considered to have an applicant residing in municipality
			externalTaskService.complete(externalTask, Map.of("isResidentOfMunicipality", NumberUtils.isCreatable(externalTask.getBusinessKey()) && NumberUtils.createLong(externalTask.getBusinessKey()) % 2 == 0));
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
