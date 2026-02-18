package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.parkingpermit.Constants.*;

class PartyAssetsMapperTest {
	private static final String ERRAND_ID = "123";
	private static final String PERMIT_NUMBER = "1234567890";
	private static final OffsetDateTime VALID_FROM = OffsetDateTime.now();
	private static final OffsetDateTime VALID_TO = VALID_FROM.plusDays(30);
	private static final String PERMIT_STATUS = "Utgånget";

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
			.decisions(List.of(new Decision().decisionType(Decision.DecisionTypeEnum.PROPOSED),
				new Decision().decisionType(Decision.DecisionTypeEnum.FINAL).validFrom(VALID_FROM).validTo(VALID_TO)));

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
		final var errand = createErrand().stakeholders(createStakeholders()).extraParameters(List.of(new ExtraParameter(CASEDATA_KEY_ARTEFACT_PERMIT_STATUS).addValuesItem("BadStatus")));
		final var exception = assertThrows(ThrowableProblem.class, () -> PartyAssetsMapper.toAssetCreateRequest(errand));

		assertThat(exception).hasMessage("Conflict: Unexpected value: BadStatus");
	}

	private Errand createErrand() {
		final var errand = new Errand();
		errand.setId(Long.valueOf(ERRAND_ID));
		errand.setDecisions(createDecisions());
		return errand;
	}

	private List<Stakeholder> createStakeholders() {
		return List.of(
			new Stakeholder()
				.id(123L)
				.type(PERSON)
				.roles(List.of(ROLE_ADMINISTRATOR))
				.personId("123"),
			new Stakeholder()
				.id(456L)
				.type(PERSON)
				.roles(List.of(ROLE_APPLICANT))
				.personId("456"));
	}

	private List<Decision> createDecisions() {
		return List.of(new Decision().decisionType(Decision.DecisionTypeEnum.PROPOSED),
			new Decision().decisionType(Decision.DecisionTypeEnum.FINAL).validFrom(VALID_FROM).validTo(VALID_TO));
	}

	private List<ExtraParameter> createExtraParameters() {
		return List.of(
			new ExtraParameter().key(CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER).values(List.of(PERMIT_NUMBER)),
			new ExtraParameter().key(CASEDATA_KEY_ARTEFACT_PERMIT_STATUS).values(List.of(PERMIT_STATUS)));
	}
}
