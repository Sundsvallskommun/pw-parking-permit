package se.sundsvall.parkingpermit.businesslogic.worker.followup;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.Optional;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;

@Component
@ExternalTaskSubscription("CleanUpNotesTask")
public class CleanUpNotesTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CleanUpNotesTask");

			final var notes = caseDataClient.getNotesByErrandId(externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER), INTERNAL.getValue());

			Optional.ofNullable(notes).orElse(emptyList()).forEach(internalNote -> caseDataClient.deleteNoteById(internalNote.getId()));

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
