package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;
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
@ExternalTaskSubscription("CheckErrandPhaseActionTask")
public class CheckErrandPhaseActionTaskWorker extends AbstractTaskWorker {

	CheckErrandPhaseActionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			clearUpdateAvailable(externalTask);
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber);
			logInfo("Check phase action for errand with id {}", errand.getId());

			final var phaseAction = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(extraParameters -> CASEDATA_KEY_PHASE_ACTION.equals(extraParameters.getKey()))
				.findFirst()
				.flatMap(extraParameters -> extraParameters.getValues().stream().findFirst())
				.orElse(PHASE_ACTION_UNKNOWN);

			final var displayPhase = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(extraParameters -> CASEDATA_KEY_DISPLAY_PHASE.equals(extraParameters.getKey()))
				.findFirst()
				.flatMap(extraParameters -> extraParameters.getValues().stream().findFirst())
				.orElse(null);

			switch (phaseAction) {
				case PHASE_ACTION_COMPLETE -> {
					logInfo("Phase action is complete. Setting phase status to {}", PHASE_STATUS_COMPLETED);
					caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_COMPLETED, phaseAction, displayPhase, errand.getExtraParameters()));
				}
				case PHASE_ACTION_CANCEL -> {
					logInfo("Phase action is cancel. Setting phase status to {}", PHASE_STATUS_CANCELED);
					caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_CANCELED, phaseAction, displayPhase, errand.getExtraParameters()));
				}
				default -> {
					logInfo("Phase action is unknown. Setting phase status to {}", PHASE_STATUS_WAITING);
					if (isPhaseStatusNotWaiting(errand)) {
						caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(), toPatchErrand(errand.getExternalCaseId(), errand.getPhase(), PHASE_STATUS_WAITING, phaseAction, displayPhase, errand.getExtraParameters()));
					}
				}
			}

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, phaseAction);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isPhaseStatusNotWaiting(Errand errand) {
		return !PHASE_STATUS_WAITING.equals(Optional.ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameters -> CASEDATA_KEY_PHASE_STATUS.equals(extraParameters.getKey()))
			.findFirst()
			.map(extraParameters -> extraParameters.getValues().getFirst())
			.orElse(null));
	}
}
