package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_SANITY_CHECK_PASSED;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.Map;
import java.util.Set;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("InvestigationSanityCheckTask")
public class SanityCheckTaskWorker extends AbstractTaskWorker {

	private static final Set<String> VALID_CASE_TYPES = Set.of(CASE_TYPE_PARKING_PERMIT, CASE_TYPE_PARKING_PERMIT_RENEWAL, CASE_TYPE_LOST_PARKING_PERMIT);

	SanityCheckTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for SanityCheckTaskWorker");
			clearUpdateAvailable(externalTask);
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final String namespace = externalTask.getVariable(CAMUNDA_VARIABLE_NAMESPACE);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing sanity check for errand with id {}", errand.getId());

			final var sanityCheckPassed = executeSanityChecks(errand);

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_SANITY_CHECK_PASSED, sanityCheckPassed));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean executeSanityChecks(Errand errand) {
		return hasErrandAdministrator(errand) &&
			hasErrandValidCaseType(errand) &&
			hasErrandStakeholderApplicant(errand);
	}

	private boolean hasErrandAdministrator(Errand errand) {
		final var hasAdministrator = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.anyMatch(rolesEnums -> rolesEnums.contains(ROLE_ADMINISTRATOR));
		if (!hasAdministrator) {
			logInfo("Errand with id {} miss an administrator", errand.getId());
		}
		return hasAdministrator;
	}

	private boolean hasErrandValidCaseType(Errand errand) {
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

	private boolean hasErrandStakeholderApplicant(Errand errand) {
		final var hasApplicant = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.anyMatch(rolesEnums -> rolesEnums.contains(ROLE_APPLICANT));

		if (!hasApplicant) {
			logInfo("Errand with id {} has no stakeholder with role APPLICANT", errand.getId());
		}
		return hasApplicant;
	}
}
