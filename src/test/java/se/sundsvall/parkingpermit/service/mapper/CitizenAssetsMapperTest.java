package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.citizenassets.AssetCreateRequest;
import generated.se.sundsvall.citizenassets.Status;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_RENEWAL_EXPERATION_DATE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;

class CitizenAssetsMapperTest {
	final static String ERRAND_ID = "123";
	final static String PERMIT_NUMBER = "1234567890";
	final static OffsetDateTime UPDATED = OffsetDateTime.now();
	final static String EXPIRATION_DATE = "2023-12-31";
	final static String PERMIT_STATUS = "Utgånget";

	@Test
	void toAssetCreateRequestWithNullErrand() {
		assertThat(CitizenAssetsMapper.toAssetCreateRequest(null)).isNull();
	}

	@Test
	void toAssetCreateRequest() {

		assertThat(CitizenAssetsMapper.toAssetCreateRequest(createErrand().stakeholders(createStakeholders()).extraParameters(createExtraParameters())))
			.isNotNull().extracting(AssetCreateRequest::getAssetId,
				AssetCreateRequest::getPartyId,
				AssetCreateRequest::getType,
				AssetCreateRequest::getDescription,
				AssetCreateRequest::getStatus,
				AssetCreateRequest::getIssued,
				AssetCreateRequest::getValidTo,
				AssetCreateRequest::getCaseReferenceIds)
			.containsExactly(PERMIT_NUMBER,
				"456",
				"PARKINGPERMIT",
				"Parkeringstillstånd",
				Status.EXPIRED,
				UPDATED.toLocalDate(),
				java.time.LocalDate.parse(EXPIRATION_DATE),
				List.of(ERRAND_ID));
	}

	@Test
	void toAssetCreateRequestNoExtraparameters() {

		assertThat(CitizenAssetsMapper.toAssetCreateRequest(createErrand().stakeholders(createStakeholders())))
			.isNotNull().extracting(AssetCreateRequest::getAssetId,
				AssetCreateRequest::getPartyId,
				AssetCreateRequest::getType,
				AssetCreateRequest::getDescription,
				AssetCreateRequest::getStatus,
				AssetCreateRequest::getIssued,
				AssetCreateRequest::getValidTo,
				AssetCreateRequest::getCaseReferenceIds)
			.containsExactly(null,
				"456",
				"PARKINGPERMIT",
				"Parkeringstillstånd",
				null,
				UPDATED.toLocalDate(),
				null,
				List.of(ERRAND_ID));
	}

	@Test
	void toAssetCreateRequestNoStakeholders() {
		// Arrange
		final var errand = createErrand().extraParameters(createExtraParameters());

		final var exception = assertThrows(ThrowableProblem.class, () -> CitizenAssetsMapper.toAssetCreateRequest(errand));

		assertThat(exception).hasMessage("Not Found: Errand is missing stakeholder with role 'APPLICANT'");
	}

	@Test
	void toAssetCreateRequestBadStatusFromCaseData() {
		// Arrange
		final var errand = createErrand().stakeholders(createStakeholders()).extraParameters(Map.of(CASEDATA_KEY_ARTEFACT_PERMIT_STATUS, "BadStatus"));
		final var exception = assertThrows(ThrowableProblem.class, () -> CitizenAssetsMapper.toAssetCreateRequest(errand));

		assertThat(exception).hasMessage("Conflict: Unexpected value: BadStatus");
	}

	private ErrandDTO createErrand() {
		final var errand = new ErrandDTO();
		errand.setId(Long.valueOf(ERRAND_ID));
		errand.setUpdated(UPDATED);
		return errand;
	}

	private List<StakeholderDTO> createStakeholders() {
		return List.of(
			new StakeholderDTO()
				.id(123L)
				.type(PERSON)
				.roles(List.of(ADMINISTRATOR))
				.personId("123"),
			new StakeholderDTO()
				.id(456L)
				.type(PERSON)
				.roles(List.of(APPLICANT))
				.personId("456")
		);
	}

	private Map<String, String> createExtraParameters() {
		return Map.of(
			CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER, PERMIT_NUMBER,
			CASEDATA_KEY_APPLICATION_RENEWAL_EXPERATION_DATE, EXPIRATION_DATE,
			CASEDATA_KEY_ARTEFACT_PERMIT_STATUS, PERMIT_STATUS
		);
	}
}
