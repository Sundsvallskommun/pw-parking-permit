package se.sundsvall.parkingpermit.util;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.NOT_FOUND;

import java.util.Optional;

import org.zalando.problem.Problem;

import generated.se.sundsvall.casedata.AddressDTO;
import generated.se.sundsvall.casedata.AddressDTO.AddressCategoryEnum;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum;

public class ErrandUtil {

	private ErrandUtil() {}

	public static StakeholderDTO getStakeholder(ErrandDTO errand, RolesEnum role) {
		return getOptionalStakeholder(errand, null, role)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format("Errand is missing stakeholder with role '%s'", role)));
	}

	public static StakeholderDTO getStakeholder(ErrandDTO errand, TypeEnum type, RolesEnum role) {
		return getOptionalStakeholder(errand, type, role)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format("Errand is missing stakeholder of type '%s' with role '%s'", type, role)));
	}

	public static Optional<StakeholderDTO> getOptionalStakeholder(ErrandDTO errand, TypeEnum type, RolesEnum role) {
		return ofNullable(errand.getStakeholders()).orElse(emptyList())
			.stream()
			.filter(stakeholder -> isNull(type) || type.equals(stakeholder.getType()))
			.filter(stakeholder -> stakeholder.getRoles().contains(role))
			.findAny();
	}

	public static Optional<AddressDTO> getAddress(StakeholderDTO stakeholder, AddressCategoryEnum addressCategory) {
		return ofNullable(stakeholder.getAddresses()).orElse(emptyList())
			.stream()
			.filter(address -> isNull(addressCategory) || addressCategory.equals(address.getAddressCategory()))
			.findAny();
	}
}
