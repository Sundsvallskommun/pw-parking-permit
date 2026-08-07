package se.sundsvall.parkingpermit.businesslogic.worker;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;

import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_STATUS;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_STATUS_DESCRIPTION;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

@Component
@ExternalTaskSubscription("UpdateErrandStatusTask")
public class UpdateErrandStatusTaskWorker extends AbstractTaskWorker {

	UpdateErrandStatusTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {
		super(engineClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing update of status for errand with id {}", errand.getId());

			final var status = Optional.ofNullable(externalTask.getVariable(PROCESS_VARIABLE_STATUS))
				.map(Object::toString)
				.orElseThrow(() -> new IllegalStateException("Process variable '%s' is not set".formatted(PROCESS_VARIABLE_STATUS)));
			final var statusDescription = Optional.ofNullable(externalTask.getVariable(PROCESS_VARIABLE_STATUS_DESCRIPTION)).map(Object::toString).orElse(status);
			caseDataClient.patchStatus(municipalityId, namespace, errand.getId(), toStatus(status, statusDescription));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
