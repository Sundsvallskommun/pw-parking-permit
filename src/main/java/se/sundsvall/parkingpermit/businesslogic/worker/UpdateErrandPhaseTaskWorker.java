package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_FINALIZED;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_ONGOING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import generated.se.sundsvall.casedata.Errand;
import java.util.HashMap;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("UpdateErrandPhaseTask")
public class UpdateErrandPhaseTaskWorker extends AbstractTaskWorker {

	UpdateErrandPhaseTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);
			final String phase = externalTask.getVariable(CAMUNDA_VARIABLE_PHASE);
			final String displayPhase = externalTask.getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing update of phase for errand with id {}", errand.getId());

			ofNullable(phase).ifPresentOrElse(
				phaseValue -> {
					final var newDisplayPhase = ofNullable(displayPhase).orElse(phaseValue);
					logInfo("Setting phase to {}", phaseValue);
					final var phaseStatus = isErrandFinalized(errand) ? PHASE_STATUS_COMPLETED : PHASE_STATUS_ONGOING;

					// Set phase action to unknown to errand in the beginning of the phase and in the end of process
					caseDataClient.patchErrand(municipalityId, namespace, errand.getId(), toPatchErrand(errand.getExternalCaseId(), phaseValue, phaseStatus, PHASE_ACTION_UNKNOWN, newDisplayPhase, errand.getExtraParameters()));
				},
				() -> logInfo("Phase is not set"));

			// Set phase action to unknown in the beginning of the phase
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_UNKNOWN);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isErrandFinalized(Errand errand) {
		return Optional.ofNullable(errand.getStatuses()).orElse(emptyList()).stream()
			.anyMatch(status -> CASEDATA_STATUS_CASE_FINALIZED.equals(status.getStatusType()));
	}
}
