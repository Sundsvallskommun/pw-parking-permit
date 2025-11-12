package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_FINALIZED;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_AUTOMATIC;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_ONGOING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toExtraParameterList;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import java.util.HashMap;
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
@ExternalTaskSubscription("UpdateErrandPhaseTask")
public class UpdateErrandPhaseTaskWorker extends AbstractTaskWorker {

	UpdateErrandPhaseTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);
			final String phase = externalTask.getVariable(CAMUNDA_VARIABLE_PHASE);
			final String displayPhase = externalTask.getVariable(CAMUNDA_VARIABLE_DISPLAY_PHASE);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing update of phase for errand with id {}", errand.getId());

			// If action is "AUTOMATIC" it should not be changed
			final var phaseAction = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(extraParameters -> CASEDATA_KEY_PHASE_ACTION.equals(extraParameters.getKey()))
				.findFirst()
				.map(ExtraParameter::getValues)
				.map(List::getFirst)
				.filter(PHASE_ACTION_AUTOMATIC::equals)
				.orElse(PHASE_ACTION_UNKNOWN);

			ofNullable(phase).ifPresentOrElse(
				phaseValue -> {
					logInfo("Setting phase to {}", phaseValue);
					final var newDisplayPhase = ofNullable(displayPhase).orElse(phaseValue);
					final var phaseStatus = isErrandFinalized(errand) ? PHASE_STATUS_COMPLETED : PHASE_STATUS_ONGOING;

					// Set phase action to unknown to errand in the beginning of the phase and in the end of process
					caseDataClient.patchErrand(municipalityId, namespace, errand.getId(), toPatchErrand(errand.getExternalCaseId(), phaseValue));
					caseDataClient.patchErrandExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameterList(phaseStatus, phaseAction, newDisplayPhase));
				},
				() -> logInfo("Phase is not set"));

			// Set phase action to unknown in the beginning of the phase
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, phaseAction);

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
