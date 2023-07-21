package se.sundsvall.parkingpermit.util;

import static generated.se.sundsvall.casedata.AddressDTO.AddressCategoryEnum.POSTAL_ADDRESS;
import static generated.se.sundsvall.casedata.AddressDTO.AddressCategoryEnum.VISITING_ADDRESS;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.CONTROL_OFFICIAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.DOCTOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.DRIVER;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.ORGANIZATION;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.casedata.AddressDTO;
import generated.se.sundsvall.casedata.AddressDTO.AddressCategoryEnum;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum;

class ErrandUtilTest {

	private static final ErrandDTO ERRAND = createErrand(List.of(
		Map.entry(PERSON, DRIVER),
		Map.entry(ORGANIZATION, CONTROL_OFFICIAL)));

	@Test
	void getStakeholderWithRoleWithMatches() {
		final var result = ErrandUtil.getStakeholder(ERRAND, CONTROL_OFFICIAL);

		assertThat(result.getType()).isEqualTo(ORGANIZATION);
		assertThat(result.getRoles()).containsExactly(CONTROL_OFFICIAL);
	}

	@Test
	void getStakeholderWithRoleWhenNoMatches() {
		final var e = assertThrows(ThrowableProblem.class, () -> ErrandUtil.getStakeholder(ERRAND, DOCTOR));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder with role 'DOCTOR'");
	}

	@Test
	void getStakeholderWithStakeholderTypeAndRoleWithMatches() {
		final var result = ErrandUtil.getStakeholder(ERRAND, ORGANIZATION, CONTROL_OFFICIAL);

		assertThat(result.getType()).isEqualTo(ORGANIZATION);
		assertThat(result.getRoles()).containsExactly(CONTROL_OFFICIAL);

	}

	@Test
	void getStakeholderWithStakeholderTypeAndRoleWhenNoMatches() {
		final var e = assertThrows(ThrowableProblem.class, () -> ErrandUtil.getStakeholder(ERRAND, PERSON, CONTROL_OFFICIAL));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Errand is missing stakeholder of type 'PERSON' with role 'CONTROL_OFFICIAL'");
	}

	@Test
	void getOptionalStakeholderWhenMatchExists() {
		assertThat(ErrandUtil.getOptionalStakeholder(ERRAND, ORGANIZATION, CONTROL_OFFICIAL)).isPresent();
	}

	@Test
	void getOptionalStakeholderWhenMatchDoesNotExist() {
		assertThat(ErrandUtil.getOptionalStakeholder(ERRAND, ORGANIZATION, DRIVER)).isEmpty();
	}

	@Test
	void getOptionalAddressWhenMatchExists() {
		assertThat(ErrandUtil.getAddress(createStakeholder(Map.entry(PERSON, DRIVER)).addAddressesItem(createAddress(VISITING_ADDRESS)), VISITING_ADDRESS)).isPresent();
	}

	@Test
	void getOptionalAddressWhenMatchDoesNotExist() {
		assertThat(ErrandUtil.getAddress(createStakeholder(Map.entry(PERSON, DRIVER)).addAddressesItem(createAddress(POSTAL_ADDRESS)), VISITING_ADDRESS)).isEmpty();
	}

	private static ErrandDTO createErrand(List<Map.Entry<TypeEnum, RolesEnum>> stakeholders) {
		return new ErrandDTO()
			.stakeholders(createStakeholders(stakeholders));
	}

	private static List<StakeholderDTO> createStakeholders(List<Map.Entry<TypeEnum, RolesEnum>> stakeholders) {
		return ofNullable(stakeholders).orElse(emptyList()).stream()
			.map(ErrandUtilTest::createStakeholder)
			.toList();
	}

	private static StakeholderDTO createStakeholder(Map.Entry<TypeEnum, RolesEnum> classification) {
		return new StakeholderDTO().type(classification.getKey()).roles(List.of(classification.getValue()));
	}

	private static AddressDTO createAddress(AddressCategoryEnum category) {
		return new AddressDTO().addressCategory(category);
	}
}
