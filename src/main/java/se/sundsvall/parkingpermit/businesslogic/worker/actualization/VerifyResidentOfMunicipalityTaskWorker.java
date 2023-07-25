package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getOptionalStakeholder;

import java.util.HashMap;
import java.util.Optional;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.citizen.CitizenClient;

@Component
@ExternalTaskSubscription("VerifyResidentOfMunicipalityTask")
public class VerifyResidentOfMunicipalityTaskWorker extends AbstractTaskWorker {

	private static final String MAIN_ADDRESS_TYPE = "POPULATION_REGISTRATION_ADDRESS";

	@Value("${common.application.municipality-id:0}")
	private String requiredMunicipalityId;

	@Autowired
	private CitizenClient citizenClient;

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for VerifyResidentOfMunicipalityTask");

			final var variables = new HashMap<String, Object>();
			final var errand = getErrand(externalTask);

			getOptionalStakeholder(errand, PERSON, APPLICANT).ifPresent(applicant -> {

				final var personId = applicant.getPersonId();

				getMunicipalityId(personId).ifPresent(applicantMunicipalityId -> {

					// If applicant belongs to another municipality, set corresponding variable to indicate this.
					if (!applicantMunicipalityId.equals(requiredMunicipalityId)) {
						logInfo("Applicant with personId:'{}' does not belong to the required municipalityId:'{}'. Applicant belongs to:'{}'",
							personId, requiredMunicipalityId, applicantMunicipalityId);

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
