package se.sundsvall.parkingpermit.businesslogic.worker.actualization;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getOptionalStakeholder;

import java.util.HashMap;

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
			final var applicant = getOptionalStakeholder(errand, PERSON, APPLICANT);

			if (applicant.isPresent()) {

				final var personId = applicant.get().getPersonId();
				final var applicantMunicipalityId = getMunicipalityId(personId);

				// If applicant belongs to another municipality, set corresponding variable to indicate this.
				if (nonNull(applicantMunicipalityId) && !applicantMunicipalityId.equals(requiredMunicipalityId)) {
					logInfo("Applicant with personId:'{}' does not belong to the required municipalityId:'{}'. Applicant belongs to:'{}'",
						personId, requiredMunicipalityId, applicantMunicipalityId);

					variables.put(CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY, true);
				}
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	/**
	 * Get municipalityId of the persons population registration address (folkbokföringsadress).
	 *
	 * @param  personId
	 * @return          the municipalityId of the person, or null if no municipalityId could be identified (due to missing
	 *                  citizen, address, etc).
	 */
	private String getMunicipalityId(String personId) {
		if (isNull(personId)) {
			return null;
		}

		return citizenClient.getCitizen(personId)
			.map(CitizenExtended::getAddresses)
			.orElse(emptyList())
			.stream()
			.filter(address -> MAIN_ADDRESS_TYPE.equals(address.getAddressType()))
			.map(CitizenAddress::getMunicipality)
			.findAny()
			.orElse(null);
	}
}