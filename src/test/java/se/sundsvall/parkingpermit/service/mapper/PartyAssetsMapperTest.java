package se.sundsvall.parkingpermit.service.mapper;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_RENEWAL_EXPERATION_DATE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;

class PartyAssetsMapperTest {
	final static String ERRAND_ID = "123";
	final static String PERMIT_NUMBER = "1234567890";
	final static OffsetDateTime UPDATED = OffsetDateTime.now();
	final static String EXPIRATION_DATE = "2023-12-31";
	final static String PERMIT_STATUS = "Utgånget";

	@Test
	void toAssetCreateRequestWithNullErrand() {
		assertThat(PartyAssetsMapper.toAssetCreateRequest(null)).isNull();
	}

	@Test
	void toAssetCreateRequest() {
		final var errand = PartyAssetsMapper.toAssetCreateRequest(createErrand().stakeholders(createStakeholders()).extraParameters(createExtraParameters()));

		assertThat(errand).isNotNull()
			.extracting(
				AssetCreateRequest::getAdditionalParameters,
				AssetCreateRequest::getAssetId,
				AssetCreateRequest::getCaseReferenceIds,
				AssetCreateRequest::getDescription,
				AssetCreateRequest::getIssued,
				AssetCreateRequest::getOrigin,
				AssetCreateRequest::getPartyId,
				AssetCreateRequest::getStatus,
				AssetCreateRequest::getType,
				AssetCreateRequest::getValidTo)
			.containsExactly(
				emptyMap(),
				PERMIT_NUMBER,
				List.of(ERRAND_ID),
				"Parkeringstillstånd",
				UPDATED.toLocalDate(),
				"CASEDATA",
				"456",
				Status.EXPIRED,
				"PARKINGPERMIT",
				LocalDate.parse(EXPIRATION_DATE));
	}

	@Test
	void toAssetCreateRequestNoExtraParameters() {
		final var errand = createErrand().stakeholders(createStakeholders());

		assertThat(PartyAssetsMapper.toAssetCreateRequest(errand)).isNotNull()
			.extracting(
				AssetCreateRequest::getAdditionalParameters,
				AssetCreateRequest::getAssetId,
				AssetCreateRequest::getCaseReferenceIds,
				AssetCreateRequest::getDescription,
				AssetCreateRequest::getIssued,
				AssetCreateRequest::getOrigin,
				AssetCreateRequest::getPartyId,
				AssetCreateRequest::getStatus,
				AssetCreateRequest::getType,
				AssetCreateRequest::getValidTo)
			.containsExactly(
				emptyMap(),
				null,
				List.of(ERRAND_ID),
				"Parkeringstillstånd",
				UPDATED.toLocalDate(),
				"CASEDATA",
				"456",
				null,
				"PARKINGPERMIT",
				null);
	}

	@Test
	void toAssetCreateRequestNoStakeholders() {
		// Arrange
		final var errand = createErrand().extraParameters(createExtraParameters());

		final var exception = assertThrows(ThrowableProblem.class, () -> PartyAssetsMapper.toAssetCreateRequest(errand));

		assertThat(exception).hasMessage("Not Found: Errand is missing stakeholder with role 'APPLICANT'");
	}

	@Test
	void toAssetCreateRequestBadStatusFromCaseData() {
		// Arrange
		final var errand = createErrand().stakeholders(createStakeholders()).extraParameters(Map.of(CASEDATA_KEY_ARTEFACT_PERMIT_STATUS, "BadStatus"));
		final var exception = assertThrows(ThrowableProblem.class, () -> PartyAssetsMapper.toAssetCreateRequest(errand));

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
