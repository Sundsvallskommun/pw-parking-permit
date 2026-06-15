package se.sundsvall.parkingpermit.businesslogic.worker.followup;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static java.util.Collections.emptyList;

@Component
@ExternalTaskSubscription("CleanUpNotesTask")
public class CleanUpNotesTaskWorker extends AbstractTaskWorker {

	CleanUpNotesTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {
		super(engineClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CleanUpNotesTask");

			final var caseNumber = getCaseNumber(externalTask);
			final var namespace = getNamespace(externalTask);
			final var municipalityId = getMunicipalityId(externalTask);

			final var notes = caseDataClient.getNotesByErrandId(municipalityId, namespace, caseNumber, INTERNAL.getValue());

			Optional.ofNullable(notes).orElse(emptyList()).forEach(internalNote -> caseDataClient.deleteNoteById(municipalityId, namespace, caseNumber, internalNote.getId()));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
