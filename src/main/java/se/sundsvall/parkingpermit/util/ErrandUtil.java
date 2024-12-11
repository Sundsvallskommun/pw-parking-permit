package se.sundsvall.parkingpermit.util;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.casedata.Address;
import generated.se.sundsvall.casedata.Address.AddressCategoryEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Stakeholder.TypeEnum;
import java.util.Optional;
import org.zalando.problem.Problem;

public final class ErrandUtil {

	private ErrandUtil() {}

	public static Stakeholder getStakeholder(Errand errand, String role) {
		return getOptionalStakeholder(errand, null, role)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Errand is missing stakeholder with role '%s'".formatted(role)));
	}

	public static Stakeholder getStakeholder(Errand errand, TypeEnum type, String role) {
		return getOptionalStakeholder(errand, type, role)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Errand is missing stakeholder of type '%s' with role '%s'".formatted(type, role)));
	}

	public static Optional<Stakeholder> getOptionalStakeholder(Errand errand, TypeEnum type, String role) {
		return ofNullable(errand.getStakeholders()).orElse(emptyList())
			.stream()
			.filter(stakeholder -> isNull(type) || type.equals(stakeholder.getType()))
			.filter(stakeholder -> stakeholder.getRoles().contains(role))
			.findAny();
	}

	public static Optional<Address> getAddress(Stakeholder stakeholder, AddressCategoryEnum addressCategory) {
		return ofNullable(stakeholder.getAddresses()).orElse(emptyList())
			.stream()
			.filter(address -> isNull(addressCategory) || addressCategory.equals(address.getAddressCategory()))
			.findAny();
	}
}
