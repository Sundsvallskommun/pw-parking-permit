package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toExtraParameterList;

@Component
@ExternalTaskSubscription("VerifyAdministratorStakeholderExists")
public class VerifyAdministratorStakeholderExistsTaskWorker extends AbstractTaskWorker {

	VerifyAdministratorStakeholderExistsTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute task for evaluating if stakeholder with role 'ADMINISTRATOR' is present.");
			clearUpdateAvailable(externalTask);

			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var administratorIsAssigned = isAdministratorAssigned(errand);
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_ASSIGNED_TO_ADMINISTRATOR, administratorIsAssigned);

			if (isCancel(errand)) {
				logInfo("Cancel has been requested for errand with id {}", errand.getId());

				caseDataClient.patchErrandExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameterList(PHASE_STATUS_CANCELED, PHASE_ACTION_CANCEL));
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);

			} else if (!administratorIsAssigned) {
				caseDataClient.patchErrandExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameterList(PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isAdministratorAssigned(Errand errand) {
		final var isControlOfficialAssigned = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.anyMatch(roles -> roles.contains("ADMINISTRATOR"));

		logInfo("Errand with id {} {} been assigned to a control official", errand.getId(), isControlOfficialAssigned ? "has" : "has not yet");
		return isControlOfficialAssigned;
	}
}
