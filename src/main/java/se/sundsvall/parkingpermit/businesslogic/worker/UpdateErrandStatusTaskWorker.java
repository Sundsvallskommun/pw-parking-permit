package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_ACTUALIZATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_CANCELED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_INVESTIGATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_CANCELED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_PROCESS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

import java.util.List;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("UpdateErrandStatusTask")
public class UpdateErrandStatusTaskWorker extends AbstractTaskWorker {

	UpdateErrandStatusTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber);
			logInfo("Executing update of status for errand with id {}", errand.getId());

			final var phase = errand.getPhase();

			switch (phase) {
				case CASEDATA_PHASE_INVESTIGATION -> caseDataClient.putStatus(municipalityId, errand.getNamespace(), errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_PROCESS, "Ärendet utreds")));
				case CASEDATA_PHASE_DECISION -> {
					if (isCitizen(externalTask) || isAppealAndInTimeLinessReview(externalTask)) {
						// Errand is in decision sub process
						caseDataClient.putStatus(municipalityId, errand.getNamespace(), errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_DECIDE, "Ärendet beslutas")));
					} else {
						// Errand is in automatic denial sub process
						caseDataClient.putStatus(municipalityId, errand.getNamespace(), errand.getId(), List.of(toStatus(CASEDATA_STATUS_DECISION_EXECUTED, "Ärendet avvisas")));
					}
				}
				case CASEDATA_PHASE_ACTUALIZATION -> {
					final var status = externalTask.getVariable("status").toString();
					final var statusDescription = Optional.ofNullable(externalTask.getVariable("statusDescription")).map(Object::toString).orElse(status);
					caseDataClient.putStatus(municipalityId, errand.getNamespace(), errand.getId(), List.of(toStatus(status, statusDescription)));
				}
				case CASEDATA_PHASE_CANCELED -> caseDataClient.putStatus(municipalityId, errand.getNamespace(), errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_CANCELED, "Processen har avbrutits")));
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
		return !Optional.ofNullable(applicantNotResidentOfMuncipality).map(Boolean.class::cast).orElse(true);
	}

	private boolean isAppealAndInTimeLinessReview(ExternalTask externalTask) {
		final var isAppeal = externalTask.getVariable(CAMUNDA_VARIABLE_IS_APPEAL);
		final var isInTimelinessReview = externalTask.getVariable(CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW);
		return Optional.ofNullable(isAppeal).map(Boolean.class::cast).orElse(false) && Optional.ofNullable(isInTimelinessReview).map(Boolean.class::cast).orElse(false);
	}
}
