package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;

class PartyAssetsMapperTest {
	final static String ERRAND_ID = "123";
	final static String PERMIT_NUMBER = "1234567890";
	final static OffsetDateTime VALID_FROM = OffsetDateTime.now();
	final static OffsetDateTime VALID_TO = VALID_FROM.plusDays(30);
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
				VALID_FROM.toLocalDate(),
				"CASEDATA",
				"456",
				Status.EXPIRED,
				"PARKINGPERMIT",
				VALID_TO.toLocalDate());
	}

	@Test
	void toAssetCreateRequestNoExtraParameters() {
		final var errand = createErrand().stakeholders(createStakeholders())
			.decisions(List.of(new DecisionDTO().decisionType(DecisionDTO.DecisionTypeEnum.PROPOSED),
				new DecisionDTO().decisionType(DecisionDTO.DecisionTypeEnum.FINAL).validFrom(VALID_FROM).validTo(VALID_TO)));

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
				VALID_FROM.toLocalDate(),
				"CASEDATA",
				"456",
				null,
				"PARKINGPERMIT",
				VALID_TO.toLocalDate());
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
		errand.setDecisions(createDecisions());
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

	private List<DecisionDTO> createDecisions() {
		return List.of(new DecisionDTO().decisionType(DecisionDTO.DecisionTypeEnum.PROPOSED),
			new DecisionDTO().decisionType(DecisionDTO.DecisionTypeEnum.FINAL).validFrom(VALID_FROM).validTo(VALID_TO));
	}

	private Map<String, String> createExtraParameters() {
		return Map.of(
			CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER, PERMIT_NUMBER,
			CASEDATA_KEY_ARTEFACT_PERMIT_STATUS, PERMIT_STATUS
		);
	}
}
