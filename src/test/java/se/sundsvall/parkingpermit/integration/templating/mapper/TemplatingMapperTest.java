package se.sundsvall.parkingpermit.integration.templating.mapper;

import generated.se.sundsvall.casedata.Address;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.templating.RenderRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import static generated.se.sundsvall.casedata.Address.AddressCategoryEnum.POSTAL_ADDRESS;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

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
	void toRenderDecisionRequestWithNullParams() {
		assertThat(TemplatingMapper.toRenderDecisionRequest(null, null)).isNull();
	}

	@Test
	void toRenderDecisionRequestWithIdentifierWithAllAttributes() {
		final var templateIdentifier = "templateIdentifier";
		assertThat(TemplatingMapper.toRenderDecisionRequest(createErrand(true), templateIdentifier))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters", "metadata")
			.hasFieldOrPropertyWithValue("identifier", templateIdentifier)
			.extracting(RenderRequest::getParameters)
			.asInstanceOf(MAP)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"addressCo", "Care Of",
				"addressLine1", "Street 123",
				"addressLine2", "81234 City",
				"addressFirstname", "Applicant First-Name",
				"addressLastname", "Applicant Lastname",
				"caseNumber", ERRAND_NUMBER,
				"creationDate", CREATED.format(ISO_LOCAL_DATE),
				"decisionDate", OffsetDateTime.now().format(ISO_LOCAL_DATE)));
	}

	@Test
	void toRenderDecisionRequestWithIdentifierWithFirstNameNull() {
		final var errand = createErrand(true);
		final var templateIdentifier = "templateIdentifier";
		errand.getStakeholders().forEach(stakeholder -> stakeholder.setFirstName(null));

		assertThat(TemplatingMapper.toRenderDecisionRequest(errand, templateIdentifier))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters", "metadata")
			.hasFieldOrPropertyWithValue("identifier", templateIdentifier)
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
	void toRenderDecisionRequestWithIdentifierWithAddressNull() {
		final var errand = createErrand(true);
		final var templateIdentifier = "templateIdentifier";
		errand.getStakeholders().forEach(stakeholder -> stakeholder.setAddresses(null));

		assertThat(TemplatingMapper.toRenderDecisionRequest(errand, templateIdentifier))
			.hasAllNullFieldsOrPropertiesExcept("identifier", "parameters", "metadata")
			.hasFieldOrPropertyWithValue("identifier", templateIdentifier)
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
	void toRenderDecisionRequestWhenApplicantStakeholderNotPresent() {
		final var errand = createErrand(false);
		final var templateIdentifier = "templateIdentifier";

		final var e = assertThrows(ThrowableProblem.class, () -> TemplatingMapper.toRenderDecisionRequest(errand, templateIdentifier));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder with role 'APPLICANT'");
	}

	private static Errand createErrand(boolean withApplicant) {
		final var errand = new Errand()
			.errandNumber(ERRAND_NUMBER)
			.created(CREATED);

		if (withApplicant) {
			errand.addStakeholdersItem(createStakeholder(ROLE_APPLICANT, true));
		}

		return errand;
	}

	private static Stakeholder createStakeholder(String role, boolean withContactAddress) {
		final var stakeholder = new Stakeholder()
			.firstName(role + " " + FIRST_NAME)
			.lastName(role + " " + LAST_NAME)
			.roles(List.of(role));

		if (withContactAddress) {
			stakeholder.addAddressesItem(createAddress());
		}

		return stakeholder;
	}

	private static Address createAddress() {
		return new Address()
			.careOf(CARE_OF)
			.addressCategory(POSTAL_ADDRESS)
			.street(STREET)
			.postalCode(POSTAL_CODE)
			.city(CITY);
	}
}
