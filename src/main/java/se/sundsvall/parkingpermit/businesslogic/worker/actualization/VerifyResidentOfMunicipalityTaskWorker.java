package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getOptionalStakeholder;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.citizen.CitizenClient;

@Component
@ExternalTaskSubscription("VerifyResidentOfMunicipalityTask")
public class VerifyResidentOfMunicipalityTaskWorker extends AbstractTaskWorker {

	static final String MAIN_ADDRESS_TYPE = "POPULATION_REGISTRATION_ADDRESS";

	private final CitizenClient citizenClient;

	VerifyResidentOfMunicipalityTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, CitizenClient citizenClient) {
		super(camundaClient, caseDataClient, failureHandler);
		this.citizenClient = citizenClient;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for VerifyResidentOfMunicipalityTask");

			final var variables = new HashMap<String, Object>(Map.of(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, false));
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			getOptionalStakeholder(errand, PERSON, ROLE_APPLICANT).ifPresent(applicant -> {

				final var personId = applicant.getPersonId();

				getMunicipalityId(personId).ifPresent(applicantMunicipalityId -> {

					// If applicant belongs to another municipality, set corresponding variable to indicate this.
					if (!applicantMunicipalityId.equals(municipalityId)) {
						logInfo("Applicant with personId:'{}' does not belong to the required municipalityId:'{}'. Applicant belongs to:'{}'",
							personId, municipalityId, applicantMunicipalityId);

						variables.put(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, true);
					}
				});
			});

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	/**
	 * Get municipalityId of the persons population registration address (folkbokföringsadress).
	 *
	 * @param  personId the personId of the citizen.
	 * @return          an Optional municipalityId for the person, or an empty Optional if no municipalityId could be
	 *                  identified (due to missing
	 *                  citizen, address, etc).
	 */
	private Optional<String> getMunicipalityId(String personId) {
		if (isBlank(personId)) {
			return empty();
		}

		return citizenClient.getCitizen(personId)
			.map(CitizenExtended::getAddresses)
			.orElse(emptyList())
			.stream()
			.filter(address -> MAIN_ADDRESS_TYPE.equals(address.getAddressType()))
			.map(CitizenAddress::getMunicipality)
			.findAny();
	}
}
