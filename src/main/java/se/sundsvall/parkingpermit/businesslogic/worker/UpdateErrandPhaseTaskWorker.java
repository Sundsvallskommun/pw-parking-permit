package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("UpdateErrandPhaseTask")
public class UpdateErrandPhaseTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			logInfo("Executing update of phase for errand with id {}", errand.getId());

			caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION));
			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
