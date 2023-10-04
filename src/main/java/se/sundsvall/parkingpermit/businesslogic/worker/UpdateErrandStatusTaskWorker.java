package se.sundsvall.parkingpermit.businesslogic.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_INVESTIGATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_PROCESS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

@Component
@ExternalTaskSubscription("UpdateErrandStatusTask")
public class UpdateErrandStatusTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			logInfo("Executing update of status for errand with id {}", errand.getId());

			final var phase = errand.getPhase();
			final var statuses = Optional.ofNullable(errand.getStatuses()).orElse(new ArrayList<>());

			switch (phase) {
				case CASEDATA_PHASE_INVESTIGATION -> {
					statuses.add(toStatus(CASEDATA_STATUS_CASE_PROCESS, "Ärendet utreds"));
					caseDataClient.putStatus(errand.getId(), statuses);
				}
				case CASEDATA_PHASE_DECISION -> {
					if (isCitizen(externalTask)) {
						// Errand is in decision sub process
						statuses.add(toStatus(CASEDATA_STATUS_CASE_DECIDE, "Ärendet beslutas"));
						caseDataClient.putStatus(errand.getId(), statuses);
					} else {
						// Errand is in automatic denial sub process
						caseDataClient.putStatus(errand.getId(), List.of(toStatus(CASEDATA_STATUS_DECISION_EXECUTED, "Ärendet avvisas")));
					}
				}
				default -> logInfo("No status update for phase {}", phase);
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isCitizen(ExternalTask externalTask) {
		final var applicantNotResidentOfMuncipality = externalTask.getVariable(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY);
		return ! Optional.ofNullable(applicantNotResidentOfMuncipality).map(Boolean.class::cast).orElse(true);
	}
}
