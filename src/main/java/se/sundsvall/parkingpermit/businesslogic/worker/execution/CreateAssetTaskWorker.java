package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

@Component
@ExternalTaskSubscription("CreateAssetTask")
public class CreateAssetTaskWorker extends AbstractTaskWorker {

	@Autowired
	private PartyAssetsService partyAssetsService;

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CreateAssetTask");
			final var errand = getErrand(externalTask);

			partyAssetsService.createAsset(errand);

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
