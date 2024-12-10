package se.sundsvall.parkingpermit.businesslogic.worker.followup;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

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

			Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
			String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);

			final var notes = caseDataClient.getNotesByErrandId(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber, INTERNAL.getValue());

			Optional.ofNullable(notes).orElse(emptyList()).forEach(internalNote -> caseDataClient.deleteNoteById(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber, internalNote.getId()));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
