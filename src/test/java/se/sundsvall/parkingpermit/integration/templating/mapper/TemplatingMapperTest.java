package se.sundsvall.parkingpermit.integration.templating.mapper;

import static generated.se.sundsvall.casedata.AddressDTO.AddressCategoryEnum.POSTAL_ADDRESS;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.casedata.AddressDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.templating.RenderRequest;

class TemplatingMapperTest {
	private static final String ERRAND_NUMBER = "errandNumber";
	private static final OffsetDateTime CREATED = OffsetDateTime.now().minusDays(7);
	private static final String FIRST_NAME = "first-name";
	private static final String LAST_NAME = "lastName";
	private static final String CARE_OF = "care of";
	private static final String STREET = "street 123";
	private static final String POSTAL_CODE = "81234";
	private static final String CITY = "CITY";

	@Test
	void toRenderRequestWithNullParams() {
		assertThat(TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality(null)).isNull();
	}

	@Test
	void testToRenderRequestWithIdentifierWithAllAttributes() {
		assertThat(TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality(createErrand(true)))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters")
			.hasFieldOrPropertyWithValue("identifier", "sbk.prh.decision.all.rejection.municipality")
			.extracting(RenderRequest::getParameters)
			.asInstanceOf(MAP)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"addressCo", "Care Of",
				"addressLine1", "Street 123",
				"addressLine2", "81234 City",
				"addressFirstname", "Applicant First-Name",
				"addressLastname", "Applicant Lastname",
				"caseNumber", "errandNumber",
				"creationDate", CREATED.format(ISO_LOCAL_DATE),
				"decisionDate", OffsetDateTime.now().format(ISO_LOCAL_DATE)));
	}

	@Test
	void testToRenderRequestWithIdentifierWithFirstNameNull() {
		final var errand = createErrand(true);
		errand.getStakeholders().forEach(stakeholder -> stakeholder.setFirstName(null));

		assertThat(TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality(errand))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters")
			.hasFieldOrPropertyWithValue("identifier", "sbk.prh.decision.all.rejection.municipality")
			.extracting(RenderRequest::getParameters)
			.asInstanceOf(MAP)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"addressCo", "Care Of",
				"addressLine1", "Street 123",
				"addressLine2", "81234 City",
				"addressLastname", "Applicant Lastname",
				"caseNumber", "errandNumber",
				"creationDate", CREATED.format(ISO_LOCAL_DATE),
				"decisionDate", OffsetDateTime.now().format(ISO_LOCAL_DATE)));
	}

	@Test
	void testToRenderRequestWithIdentifierWithAddressNull() {
		final var errand = createErrand(true);
		errand.getStakeholders().forEach(stakeholder -> stakeholder.setAddresses(null));

		assertThat(TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality(errand))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters")
			.hasFieldOrPropertyWithValue("identifier", "sbk.prh.decision.all.rejection.municipality")
			.extracting(RenderRequest::getParameters)
			.asInstanceOf(MAP)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"addressFirstname", "Applicant First-Name",
				"addressLastname", "Applicant Lastname",
				"caseNumber", "errandNumber",
				"creationDate", CREATED.format(ISO_LOCAL_DATE),
				"decisionDate", OffsetDateTime.now().format(ISO_LOCAL_DATE)));
	}

	@Test
	void testToRenderRequestWhenApplicantStakeholderNotPresent() {
		final var errand = createErrand(false);

		final var e = assertThrows(ThrowableProblem.class, () -> TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality(errand));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder with role 'APPLICANT'");
	}

	private static ErrandDTO createErrand(boolean withApplicant) {
		final var errand = new ErrandDTO()
			.errandNumber(ERRAND_NUMBER)
			.created(CREATED);

		if (withApplicant) {
			errand.addStakeholdersItem(createStakeholder(APPLICANT, true));
		}

		return errand;
	}

	private static StakeholderDTO createStakeholder(RolesEnum role, boolean withContactAddress) {
		final var stakeholder = new StakeholderDTO()
			.firstName(role + " " + FIRST_NAME)
			.lastName(role + " " + LAST_NAME)
			.roles(List.of(role));

		if (withContactAddress) {
			stakeholder.addAddressesItem(createAddress());
		}

		return stakeholder;
	}

	private static AddressDTO createAddress() {
		return new AddressDTO()
			.careOf(CARE_OF)
			.addressCategory(POSTAL_ADDRESS)
			.street(STREET)
			.postalCode(POSTAL_CODE)
			.city(CITY);
	}
}
