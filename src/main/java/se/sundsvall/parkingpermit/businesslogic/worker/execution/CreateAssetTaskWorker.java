package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;
import se.sundsvall.parkingpermit.service.PartyAssetsService;

@Component
@ExternalTaskSubscription("CreateAssetTask")
public class CreateAssetTaskWorker extends AbstractTaskWorker {

	private final PartyAssetsService partyAssetsService;

	CreateAssetTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler, final PartyAssetsService partyAssetService) {
		super(engineClient, caseDataClient, failureHandler);
		this.partyAssetsService = partyAssetService;
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CreateAssetTask");
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			partyAssetsService.createAsset(municipalityId, namespace, errand);

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
