package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.Map;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_SANITY_CHECK_PASSED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_COMPLETION_RECEIVED;

@Component
@ExternalTaskSubscription("InvestigationSanityCheckTask")
public class SanityCheckTaskWorker extends AbstractTaskWorker {
	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for SanityCheckTaskWorker");
			clearUpdateAvailable(externalTask);

			final var errand = getErrand(externalTask);
			logInfo("Executing sanity check for errand with id {}", errand.getId());

			final var sanityCheckPassed = executeSanityChecks(errand);

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_SANITY_CHECK_PASSED, sanityCheckPassed));
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean executeSanityChecks(ErrandDTO errand) {
		return errand.getStatuses().stream().allMatch(status -> status.getStatusType().equals(Constants.CASEDATA_STATUS_CASE_RECEIVED)
			|| status.getStatusType().equals(CASEDATA_STATUS_COMPLETION_RECEIVED))
			&& checkIfErrandHasManager(errand);
	}

	private boolean checkIfErrandHasManager(ErrandDTO errand) {
		return errand.getStakeholders().stream()
			.filter(stakeholder -> PERSON.equals(stakeholder.getType()))
			.anyMatch(stakeholder -> stakeholder.getRoles().stream().anyMatch(ADMINISTRATOR::equals)); //TODO Check if this is correct
	}
}
