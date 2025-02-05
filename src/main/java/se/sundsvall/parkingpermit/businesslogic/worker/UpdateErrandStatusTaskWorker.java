package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_ACTUALIZATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_CANCELED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_FOLLOW_UP;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_INVESTIGATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_FINALIZED;
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
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing update of status for errand with id {}", errand.getId());

			final var phase = errand.getPhase();

			switch (phase) {
				case CASEDATA_PHASE_INVESTIGATION -> caseDataClient.putStatus(municipalityId, namespace, errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_PROCESS, "Ärendet utreds")));
				case CASEDATA_PHASE_DECISION -> {
					if (isCitizen(externalTask) || isAppeal(externalTask)) {
						// Errand is in decision sub process
						caseDataClient.putStatus(municipalityId, namespace, errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_DECIDE, "Ärendet beslutas")));
					} else {
						// Errand is in automatic denial sub process
						caseDataClient.putStatus(municipalityId, namespace, errand.getId(), List.of(toStatus(CASEDATA_STATUS_DECISION_EXECUTED, "Ärendet avvisas")));
					}
				}
				case CASEDATA_PHASE_ACTUALIZATION, CASEDATA_PHASE_FOLLOW_UP -> {
					final var status = externalTask.getVariable("status").toString();
					final var statusDescription = Optional.ofNullable(externalTask.getVariable("statusDescription")).map(Object::toString).orElse(status);
					caseDataClient.putStatus(municipalityId, namespace, errand.getId(), List.of(toStatus(status, statusDescription)));
				}
				case CASEDATA_PHASE_CANCELED -> caseDataClient.putStatus(municipalityId, namespace, errand.getId(), List.of(toStatus(CASEDATA_STATUS_CASE_FINALIZED, "Processen har avbrutits")));
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

	private boolean isAppeal(ExternalTask externalTask) {

		return Optional.ofNullable(externalTask.getVariable(CAMUNDA_VARIABLE_IS_APPEAL)).map(Boolean.class::cast).orElse(false);
	}
}
