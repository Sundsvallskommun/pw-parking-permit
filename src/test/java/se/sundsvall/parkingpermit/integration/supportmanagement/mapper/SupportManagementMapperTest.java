package se.sundsvall.parkingpermit.integration.supportmanagement.mapper;

import static generated.se.sundsvall.supportmanagement.Priority.MEDIUM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import generated.se.sundsvall.casedata.Address;
import generated.se.sundsvall.casedata.ContactInformation;
import generated.se.sundsvall.supportmanagement.Classification;
import generated.se.sundsvall.supportmanagement.ContactChannel;
import generated.se.sundsvall.supportmanagement.Stakeholder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SupportManagementMapperTest {

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void toSupportManagementMailingErrand(boolean isAutomatic) {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();

		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(caseDataErrand, isAutomatic);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Utskick av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.MAILING");
		if (isAutomatic) {
			assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		} else {
			assertThat(errand.getChannel()).isNull();
		}
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				"Street HouseNumber",
				"12345",
				"City",
				"CareOf",
				"SE",
				List.of(new ContactChannel().type("Email").value("a.b@c.se"),
					new ContactChannel().type("Phone").value("0701234567"))));
	}

	@Test
	void toSupportManagementMailingErrandWhenNoStreet() {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();
		caseDataErrand.getStakeholders().getFirst().getAddresses().getFirst().street(null);

		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(caseDataErrand, true);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Utskick av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.MAILING");
		assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				null,
				"12345",
				"City",
				"CareOf",
				"SE",
				List.of(new ContactChannel().type("Email").value("a.b@c.se"),
					new ContactChannel().type("Phone").value("0701234567"))));
	}

	@Test
	void toSupportManagementMailingErrandWhenNoHouseNumber() {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();
		caseDataErrand.getStakeholders().getFirst().getAddresses().getFirst().houseNumber(null);

		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(caseDataErrand, true);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Utskick av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.MAILING");
		assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				"Street",
				"12345",
				"City",
				"CareOf",
				"SE",
				List.of(new ContactChannel().type("Email").value("a.b@c.se"),
					new ContactChannel().type("Phone").value("0701234567"))));
	}

	@Test
	void toSupportManagementMailingErrandWhenEmptyAddresses() {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();
		caseDataErrand.getStakeholders().getFirst().addresses(emptyList());

		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(caseDataErrand, true);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Utskick av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.MAILING");
		assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				null,
				null,
				null,
				null,
				null,
				List.of(new ContactChannel().type("Email").value("a.b@c.se"),
					new ContactChannel().type("Phone").value("0701234567"))));
	}

	@Test
	void toSupportManagementMailingErrandWhenEmptyContactInformation() {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();
		caseDataErrand.getStakeholders().getFirst().contactInformation(emptyList());

		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(caseDataErrand, true);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Utskick av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Utskick av bifogat beslut om parkeringstillstånd ska ske via post av kontaktcenter. Gäller ärende: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.MAILING");
		assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				"Street HouseNumber",
				"12345",
				"City",
				"CareOf",
				"SE",
				emptyList()));
	}

	@Test
	void toSupportManagementMailingErrandWhenNull() {
		// Act
		final var errand = SupportManagementMapper.toSupportManagementMailingErrand(null, false);

		// Assert
		assertThat(errand).isNull();
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void toSupportManagementCardManagementErrand(boolean isAutomatic) {
		// Arrange
		final var caseDataErrand = createCaseDataErrand();

		// Act
		final var errand = SupportManagementMapper.toSupportManagementCardManagementErrand(caseDataErrand, isAutomatic);

		// Assert
		assertThat(errand.getBusinessRelated()).isFalse();
		assertThat(errand.getStatus()).isEqualTo("NEW");
		assertThat(errand.getTitle()).isEqualTo("Korthantering av parkeringstillstånd");
		assertThat(errand.getDescription()).isEqualTo("Hantering av kortet gällande parkeringstillstånd ska ske av kontaktcenter: errandNumber");
		assertThat(errand.getClassification()).isEqualTo(new Classification().category("URBAN_DEVELOPMENT").type("URBAN_DEVELOPMENT.PARKING_PERMIT"));
		assertThat(errand.getLabels()).containsExactly("URBAN_DEVELOPMENT", "URBAN_DEVELOPMENT.PARKING_PERMIT", "URBAN_DEVELOPMENT.PARKING_PERMIT.CARD_MANAGEMENT");
		if (isAutomatic) {
			assertThat(errand.getChannel()).isEqualTo("ESERVICE");
		} else {
			assertThat(errand.getChannel()).isNull();
		}
		assertThat(errand.getPriority()).isEqualTo(MEDIUM);
		assertThat(errand.getReporterUserId()).isEqualTo("adAccount");
		assertThat(errand.getActiveNotifications()).isNull();
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalIdType,
				Stakeholder::getExternalId,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRole,
				Stakeholder::getAddress,
				Stakeholder::getZipCode,
				Stakeholder::getCity,
				Stakeholder::getCareOf,
				Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.containsExactly(tuple("PRIVATE",
				"personId",
				"Firstname",
				"Lastname",
				"CONTACT",
				"Street HouseNumber",
				"12345",
				"City",
				"CareOf",
				"SE",
				List.of(new ContactChannel().type("Email").value("a.b@c.se"),
					new ContactChannel().type("Phone").value("0701234567"))));
	}

	@Test
	void toSupportManagementCardManagementErrandWhenNull() {
		// Act
		final var errand = SupportManagementMapper.toSupportManagementCardManagementErrand(null, true);

		// Assert
		assertThat(errand).isNull();
	}

	private generated.se.sundsvall.casedata.Errand createCaseDataErrand() {
		return new generated.se.sundsvall.casedata.Errand()
			.id(123L)
			.errandNumber("errandNumber")
			.municipalityId("municipalityId")
			.stakeholders(List.of(createCaseDataApplicantStakeholder(), createCaseDataAdministratorStakeholder()))
			.namespace("namespace");
	}

	private generated.se.sundsvall.casedata.Stakeholder createCaseDataApplicantStakeholder() {
		return new generated.se.sundsvall.casedata.Stakeholder()
			.personId("personId")
			.firstName("Firstname")
			.lastName("Lastname")
			.roles(List.of("APPLICANT"))
			.addresses(List.of(createCaseDataAddress()))
			.contactInformation(List.of(createCaseDataContactInformationPhone(), createCaseDataContactInformationEmail()));
	}

	private generated.se.sundsvall.casedata.Stakeholder createCaseDataAdministratorStakeholder() {
		return new generated.se.sundsvall.casedata.Stakeholder()
			.personId("personId")
			.firstName("Firstname")
			.lastName("Lastname")
			.roles(List.of("ADMINISTRATOR"))
			.adAccount("adAccount")
			.addresses(List.of(createCaseDataAddress()))
			.contactInformation(List.of(createCaseDataContactInformationPhone(), createCaseDataContactInformationEmail()));
	}

	private Address createCaseDataAddress() {
		return new Address()
			.street("Street")
			.houseNumber("HouseNumber")
			.postalCode("12345")
			.city("City")
			.careOf("CareOf")
			.country("SE");
	}

	private ContactInformation createCaseDataContactInformationPhone() {
		return new ContactInformation()
			.contactType(ContactInformation.ContactTypeEnum.PHONE)
			.value("0701234567");
	}

	private ContactInformation createCaseDataContactInformationEmail() {
		return new ContactInformation()
			.contactType(ContactInformation.ContactTypeEnum.EMAIL)
			.value("a.b@c.se");
	}
}
