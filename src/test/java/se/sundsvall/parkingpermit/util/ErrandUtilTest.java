package se.sundsvall.parkingpermit.util;

import generated.se.sundsvall.casedata.Address;
import generated.se.sundsvall.casedata.Address.AddressCategoryEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Stakeholder.TypeEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import static generated.se.sundsvall.casedata.Address.AddressCategoryEnum.POSTAL_ADDRESS;
import static generated.se.sundsvall.casedata.Address.AddressCategoryEnum.VISITING_ADDRESS;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.ORGANIZATION;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.problem.Status.NOT_FOUND;

class ErrandUtilTest {

	private static final String ROLE_CONTROL_OFFICIAL = "CONTROL_OFFICIAL";
	private static final String ROLE_DRIVER = "DRIVER";
	private static final String ROLE_DOCTOR = "DOCTOR";

	private static final Errand ERRAND = createErrand(List.of(
		Map.entry(PERSON, ROLE_DRIVER),
		Map.entry(ORGANIZATION, ROLE_CONTROL_OFFICIAL)));

	@Test
	void getStakeholderWithRoleWithMatches() {
		final var result = ErrandUtil.getStakeholder(ERRAND, ROLE_CONTROL_OFFICIAL);

		assertThat(result.getType()).isEqualTo(ORGANIZATION);
		assertThat(result.getRoles()).containsExactly(ROLE_CONTROL_OFFICIAL);
	}

	@Test
	void getStakeholderWithRoleWhenNoMatches() {
		final var e = assertThrows(ThrowableProblem.class, () -> ErrandUtil.getStakeholder(ERRAND, ROLE_DOCTOR));

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder with role 'DOCTOR'");
	}

	@Test
	void getStakeholderWithStakeholderTypeAndRoleWithMatches() {
		final var result = ErrandUtil.getStakeholder(ERRAND, ORGANIZATION, ROLE_CONTROL_OFFICIAL);

		assertThat(result.getType()).isEqualTo(ORGANIZATION);
		assertThat(result.getRoles()).containsExactly(ROLE_CONTROL_OFFICIAL);

	}

	@Test
	void getStakeholderWithStakeholderTypeAndRoleWhenNoMatches() {
		final var e = assertThrows(ThrowableProblem.class, () -> ErrandUtil.getStakeholder(ERRAND, PERSON, ROLE_CONTROL_OFFICIAL));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder of type 'PERSON' with role 'CONTROL_OFFICIAL'");
	}

	@Test
	void getOptionalStakeholderWhenMatchExists() {
		assertThat(ErrandUtil.getOptionalStakeholder(ERRAND, ORGANIZATION, ROLE_CONTROL_OFFICIAL)).isPresent();
	}

	@Test
	void getOptionalStakeholderWhenMatchDoesNotExist() {
		assertThat(ErrandUtil.getOptionalStakeholder(ERRAND, ORGANIZATION, ROLE_DRIVER)).isEmpty();
	}

	@Test
	void getOptionalAddressWhenMatchExists() {
		assertThat(ErrandUtil.getAddress(createStakeholder(Map.entry(PERSON, ROLE_DRIVER)).addAddressesItem(createAddress(VISITING_ADDRESS)), VISITING_ADDRESS)).isPresent();
	}

	@Test
	void getOptionalAddressWhenMatchDoesNotExist() {
		assertThat(ErrandUtil.getAddress(createStakeholder(Map.entry(PERSON, ROLE_DRIVER)).addAddressesItem(createAddress(POSTAL_ADDRESS)), VISITING_ADDRESS)).isEmpty();
	}

	private static Errand createErrand(List<Map.Entry<TypeEnum, String>> stakeholders) {
		return new Errand()
			.stakeholders(createStakeholders(stakeholders));
	}

	private static List<Stakeholder> createStakeholders(List<Map.Entry<TypeEnum, String>> stakeholders) {
		return ofNullable(stakeholders).orElse(emptyList()).stream()
			.map(ErrandUtilTest::createStakeholder)
			.toList();
	}

	private static Stakeholder createStakeholder(Map.Entry<TypeEnum, String> classification) {
		return new Stakeholder().type(classification.getKey()).roles(List.of(classification.getValue()));
	}

	private static Address createAddress(AddressCategoryEnum category) {
		return new Address().addressCategory(category);
	}
}
