package se.sundsvall.parkingpermit.integration.templating.mapper;

import static generated.se.sundsvall.casedata.Address.AddressCategoryEnum.POSTAL_ADDRESS;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.text.WordUtils.capitalizeFully;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getAddress;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.templating.RenderRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class TemplatingMapper {

	private static final String TEMPLATE_IDENTIFIER = "sbk.prh.decision.all.rejection.municipality";
	private static final String ADDRESS_CO = "addressCo";
	private static final String ADDRESS_FIRSTNAME = "addressFirstname";
	private static final String ADDRESS_LASTNAME = "addressLastname";
	private static final String ADDRESS_LINE_1 = "addressLine1";
	private static final String ADDRESS_LINE_2 = "addressLine2";
	private static final String CASE_NUMBER = "caseNumber";
	private static final String CREATION_DATE = "creationDate";
	private static final String DECISION_DATE = "decisionDate";

	private TemplatingMapper() {}

	public static RenderRequest toRenderRequestWhenNotMemberOfMunicipality(Errand errand) {
		if (isNull(errand)) {
			return null;
		}

		final var applicant = getStakeholder(errand, ROLE_APPLICANT);

		final var request = new RenderRequest();
		ofNullable(capitalize(applicant.getFirstName())).ifPresent(value -> request.putParametersItem(ADDRESS_FIRSTNAME, value));
		ofNullable(capitalize(applicant.getLastName())).ifPresent(value -> request.putParametersItem(ADDRESS_LASTNAME, value));
		ofNullable(errand.getCreated()).ifPresent(value -> request.putParametersItem(CREATION_DATE, value.format(ISO_LOCAL_DATE)));
		ofNullable(errand.getErrandNumber()).ifPresent(value -> request.putParametersItem(CASE_NUMBER, value));
		getAddress(applicant, POSTAL_ADDRESS).ifPresent(address -> {
			ofNullable(capitalize(address.getCareOf())).ifPresent(value -> request.putParametersItem(ADDRESS_CO, value));
			ofNullable(capitalize(address.getStreet())).ifPresent(value -> request.putParametersItem(ADDRESS_LINE_1, value));
			ofNullable(capitalize(address.getPostalCode(), address.getCity())).ifPresent(value -> request.putParametersItem(ADDRESS_LINE_2, value));
		});

		return request
			.identifier(TEMPLATE_IDENTIFIER)
			.putParametersItem(DECISION_DATE, OffsetDateTime.now(ZoneId.systemDefault()).format(ISO_LOCAL_DATE));
	}

	private static String capitalize(String... strings) {
		final var result = Arrays.stream(strings)
			.map(Arrays::asList)
			.map(list -> StringUtils.join(list, " "))
			.map(StringUtils::lowerCase)
			.map(string -> capitalizeFully(string, ' ', '-'))
			.collect(Collectors.joining(" "))
			.trim();

		return isBlank(result) ? null : result;
	}
}
