package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.LOST_PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT;
import static generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum.PARKING_PERMIT_RENEWAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_SANITY_CHECK_PASSED;

import java.util.EnumSet;
import java.util.Map;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.ErrandDTO.CaseTypeEnum;
import generated.se.sundsvall.casedata.StakeholderDTO;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("InvestigationSanityCheckTask")
public class SanityCheckTaskWorker extends AbstractTaskWorker {

	private static final EnumSet<CaseTypeEnum> VALID_CASE_TYPES = EnumSet.of(PARKING_PERMIT, PARKING_PERMIT_RENEWAL, LOST_PARKING_PERMIT);

	SanityCheckTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for SanityCheckTaskWorker");
			clearUpdateAvailable(externalTask);

			final var errand = getErrand(externalTask);
			logInfo("Executing sanity check for errand with id {}", errand.getId());

			final var sanityCheckPassed = executeSanityChecks(errand);

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_SANITY_CHECK_PASSED, sanityCheckPassed));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean executeSanityChecks(ErrandDTO errand) {
		return hasErrandAdministrator(errand) &&
			hasErrandValidCaseType(errand) &&
			hasErrandStakeholderApplicant(errand);
	}

	private boolean hasErrandAdministrator(ErrandDTO errand) {
		final var hasAdministrator = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(StakeholderDTO::getRoles)
			.anyMatch(rolesEnums -> rolesEnums.contains(ADMINISTRATOR));
		if (!hasAdministrator) {
			logInfo("Errand with id {} miss an administrator", errand.getId());
		}
		return hasAdministrator;
	}

	private boolean hasErrandValidCaseType(ErrandDTO errand) {
		if (errand.getCaseType() == null) {
			logInfo("Errand with id {} has no case type", errand.getId());
			return false;

		}
		final var hasValidCaseType = VALID_CASE_TYPES.contains(errand.getCaseType());

		if (!hasValidCaseType) {
			logInfo("Case type {} of errand with id {} is not supported", errand.getCaseType(), errand.getId());
		}
		return hasValidCaseType;
	}

	private boolean hasErrandStakeholderApplicant(ErrandDTO errand) {
		final var hasApplicant = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(StakeholderDTO::getRoles)
			.anyMatch(rolesEnums -> rolesEnums.contains(APPLICANT));

		if (!hasApplicant) {
			logInfo("Errand with id {} has no stakeholder with role APPLICANT", errand.getId());
		}
		return hasApplicant;
	}
}
