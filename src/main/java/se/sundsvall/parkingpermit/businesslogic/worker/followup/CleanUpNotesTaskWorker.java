package se.sundsvall.parkingpermit.businesslogic.worker.followup;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static java.util.Collections.emptyList;

@Component
@ExternalTaskSubscription("CleanUpNotesTask")
public class CleanUpNotesTaskWorker extends AbstractTaskWorker {

	CleanUpNotesTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CleanUpNotesTask");

			final Long caseNumber = getCaseNumber(externalTask);
			final String namespace = getNamespace(externalTask);
			final String municipalityId = getMunicipalityId(externalTask);

			final var notes = caseDataClient.getNotesByErrandId(municipalityId, namespace, caseNumber, INTERNAL.getValue());

			Optional.ofNullable(notes).orElse(emptyList()).forEach(internalNote -> caseDataClient.deleteNoteById(municipalityId, namespace, caseNumber, internalNote.getId()));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
