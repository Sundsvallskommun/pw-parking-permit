package se.sundsvall.parkingpermit.integration.supportmanagement.mapper;

import static generated.se.sundsvall.supportmanagement.Priority.MEDIUM;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.Constants.SM_CATEGORY_URBAN_DEVELOPMENT;
import static se.sundsvall.parkingpermit.Constants.SM_CONTACT_CHANNEL_TYPE_EMAIL;
import static se.sundsvall.parkingpermit.Constants.SM_CONTACT_CHANNEL_TYPE_PHONE;
import static se.sundsvall.parkingpermit.Constants.SM_DESCRIPTION_CARD_MANAGEMENT;
import static se.sundsvall.parkingpermit.Constants.SM_DESCRIPTION_MAILING_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.SM_EXTERNAL_ID_TYPE_PRIVATE;
import static se.sundsvall.parkingpermit.Constants.SM_LABEL_CARD_MANAGEMENT;
import static se.sundsvall.parkingpermit.Constants.SM_LABEL_MAILING;
import static se.sundsvall.parkingpermit.Constants.SM_ROLE_CONTACT_PERSON;
import static se.sundsvall.parkingpermit.Constants.SM_STATUS_NEW;
import static se.sundsvall.parkingpermit.Constants.SM_SUBJECT_CARD_MANAGEMENT_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.SM_SUBJECT_MAILING_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.SM_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.ContactInformation;
import generated.se.sundsvall.supportmanagement.Classification;
import generated.se.sundsvall.supportmanagement.ContactChannel;
import generated.se.sundsvall.supportmanagement.Errand;
import generated.se.sundsvall.supportmanagement.Stakeholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SupportManagementMapper {

	private static final String PROCESS_ENGINE_USER = "ProcessEngine";

	private SupportManagementMapper() {
		// Private constructor to prevent instantiation
	}

	public static Errand toSupportManagementMailingErrand(generated.se.sundsvall.casedata.Errand caseDataErrand) {
		if (caseDataErrand == null) {
			return null;
		}
		final var administratorStakeholder = getStakeholder(caseDataErrand, ROLE_ADMINISTRATOR);

		return new Errand()
			.status(SM_STATUS_NEW)
			.title(SM_SUBJECT_MAILING_PARKING_PERMIT)
			.priority(MEDIUM)
			.description(String.format(SM_DESCRIPTION_MAILING_PARKING_PERMIT, caseDataErrand.getErrandNumber()))
			.classification(new Classification().category(SM_CATEGORY_URBAN_DEVELOPMENT).type(SM_TYPE_PARKING_PERMIT))
			.labels(List.of(SM_LABEL_MAILING))
			// TODO: Check channel
			.channel("ESERVICE")
			.stakeholders(List.of(getContactStakeholderFromApplicant(caseDataErrand)))
			.reporterUserId(Optional.ofNullable(administratorStakeholder.getAdAccount()).orElse(PROCESS_ENGINE_USER))
			.activeNotifications(null)
			.businessRelated(false);
	}

	public static Errand toSupportManagementCardManagementErrand(generated.se.sundsvall.casedata.Errand caseDataErrand) {
		if (caseDataErrand == null) {
			return null;
		}

		final var administratorStakeholder = getStakeholder(caseDataErrand, ROLE_ADMINISTRATOR);

		return new Errand()
			.status(SM_STATUS_NEW)
			.title(SM_SUBJECT_CARD_MANAGEMENT_PARKING_PERMIT)
			.priority(MEDIUM)
			.description(String.format(SM_DESCRIPTION_CARD_MANAGEMENT, caseDataErrand.getErrandNumber()))
			.classification(new Classification().category(SM_CATEGORY_URBAN_DEVELOPMENT).type(SM_TYPE_PARKING_PERMIT))
			.labels(List.of(SM_LABEL_CARD_MANAGEMENT))
			// TODO: Check channel
			.channel("ESERVICE")
			.stakeholders(List.of(getContactStakeholderFromApplicant(caseDataErrand)))
			.reporterUserId(Optional.ofNullable(administratorStakeholder.getAdAccount()).orElse(PROCESS_ENGINE_USER))
			.activeNotifications(null)
			.businessRelated(false);
	}

	private static Stakeholder getContactStakeholderFromApplicant(generated.se.sundsvall.casedata.Errand caseDataErrand) {
		final var caseDataStakeholder = getStakeholder(caseDataErrand, ROLE_APPLICANT);
		return new Stakeholder()
			.externalIdType(SM_EXTERNAL_ID_TYPE_PRIVATE)
			.externalId(caseDataStakeholder.getPersonId())
			.role(SM_ROLE_CONTACT_PERSON)
			.firstName(caseDataStakeholder.getFirstName())
			.lastName(caseDataStakeholder.getLastName())
			.address(getAddress(caseDataStakeholder.getAddresses()))
			.zipCode(getZipCode(caseDataStakeholder.getAddresses()))
			.careOf(getCareOf(caseDataStakeholder.getAddresses()))
			.city(getCity(caseDataStakeholder.getAddresses()))
			.country(getCountry(caseDataStakeholder.getAddresses()))
			.contactChannels(getContactChannels(caseDataStakeholder.getContactInformation()));
	}

	private static String getAddress(List<generated.se.sundsvall.casedata.Address> caseDataAddresses) {
		if (caseDataAddresses == null || caseDataAddresses.isEmpty()) {
			return null;
		}
		final var street = caseDataAddresses.getFirst().getStreet();
		final var number = caseDataAddresses.getFirst().getHouseNumber();

		if (!isBlank(street) && !isBlank(number)) {
			return street + " " + number;
		} else if (!isBlank(street)) {
			return street;
		}
		return null;
	}

	private static String getZipCode(List<generated.se.sundsvall.casedata.Address> caseDataAddresses) {
		return Optional.ofNullable(caseDataAddresses)
			.filter(addresses -> !addresses.isEmpty())
			.map(addresses -> addresses.getFirst().getPostalCode())
			.orElse(null);
	}

	private static String getCity(List<generated.se.sundsvall.casedata.Address> caseDataAddresses) {
		return Optional.ofNullable(caseDataAddresses)
			.filter(addresses -> !addresses.isEmpty())
			.map(addresses -> addresses.getFirst().getCity())
			.orElse(null);
	}

	private static String getCareOf(List<generated.se.sundsvall.casedata.Address> caseDataAddresses) {
		return Optional.ofNullable(caseDataAddresses)
			.filter(addresses -> !addresses.isEmpty())
			.map(addresses -> addresses.getFirst().getCareOf())
			.orElse(null);
	}

	private static String getCountry(List<generated.se.sundsvall.casedata.Address> caseDataAddresses) {
		return Optional.ofNullable(caseDataAddresses)
			.filter(addresses -> !addresses.isEmpty())
			.map(addresses -> addresses.getFirst().getCountry())
			.orElse(null);
	}

	private static List<ContactChannel> getContactChannels(List<ContactInformation> contactInformations) {
		if (contactInformations == null || contactInformations.isEmpty()) {
			return emptyList();
		}
		final var emails = contactInformations.stream()
			.filter(contactInformation -> ContactInformation.ContactTypeEnum.EMAIL.equals(contactInformation.getContactType()))
			.map(ContactInformation::getValue)
			.map(email -> new ContactChannel().type(SM_CONTACT_CHANNEL_TYPE_EMAIL).value(email))
			.toList();
		final var phones = contactInformations.stream()
			.filter(contactInformation -> ContactInformation.ContactTypeEnum.PHONE.equals(contactInformation.getContactType()) ||
				ContactInformation.ContactTypeEnum.CELLPHONE.equals(contactInformation.getContactType()))
			.map(ContactInformation::getValue)
			.map(phone -> new ContactChannel().type(SM_CONTACT_CHANNEL_TYPE_PHONE).value(phone))
			.toList();

		ArrayList<ContactChannel> contactChannels = new ArrayList<>();
		contactChannels.addAll(emails);
		contactChannels.addAll(phones);
		return contactChannels;
	}
}
