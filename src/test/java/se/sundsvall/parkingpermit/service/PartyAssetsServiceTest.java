package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.PROPOSED;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

@ExtendWith(MockitoExtension.class)
class PartyAssetsServiceTest {

	final static String ERRAND_ID = "123";
	final static String PERMIT_NUMBER = "1234567890";
	final static OffsetDateTime VALID_FROM = OffsetDateTime.now();
	final static OffsetDateTime VALID_TO = VALID_FROM.plusDays(30);

	@Mock
	private PartyAssetsClient partyAssetsClientMock;

	@Captor
	private ArgumentCaptor<AssetCreateRequest> assetCreateRequestArgumentCaptor;

	@InjectMocks
	private PartyAssetsService partyAssetsService;

	@Test
	void createAsset() {

		// Arrange
		final var errand = createErrand();
		// Act
		partyAssetsService.createAsset(errand);

		// Assert
		verify(partyAssetsClientMock).createAsset(assetCreateRequestArgumentCaptor.capture());
		final var assetCreateRequest = assetCreateRequestArgumentCaptor.getValue();
		assertThat(assetCreateRequest).isNotNull();
		assertThat(assetCreateRequest.getAssetId()).isEqualTo(PERMIT_NUMBER);
		assertThat(assetCreateRequest.getPartyId()).isEqualTo(getStakeholder(errand, APPLICANT).getPersonId());
		assertThat(assetCreateRequest.getCaseReferenceIds()).containsExactly(Long.toString(errand.getId()));
		assertThat(assetCreateRequest.getType()).isEqualTo("PARKINGPERMIT");
		assertThat(assetCreateRequest.getIssued()).isEqualTo(VALID_FROM.toLocalDate());
		assertThat(assetCreateRequest.getValidTo()).isEqualTo(VALID_TO.toLocalDate());
		assertThat(assetCreateRequest.getStatus()).hasToString("ACTIVE");
		assertThat(assetCreateRequest.getDescription()).isEqualTo("Parkeringstillstånd");
	}

	private static ErrandDTO createErrand() {
		return new ErrandDTO()
			.id(Long.valueOf(ERRAND_ID))
			.decisions(List.of(createDecision(PROPOSED),createDecision(FINAL)))
			.stakeholders(List.of(createStakeholder(APPLICANT), createStakeholder(ADMINISTRATOR)))
			.extraParameters(Map.of("artefact.permit.number", PERMIT_NUMBER,
									"artefact.permit.status", "Aktivt")
			);
	}

	public static StakeholderDTO createStakeholder(RolesEnum role) {
		return new StakeholderDTO()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}

	public static DecisionDTO createDecision(DecisionDTO.DecisionTypeEnum decisionType) {
		return new DecisionDTO()
			.decisionType(decisionType)
			.validFrom(VALID_FROM)
			.validTo(VALID_TO);
	}
}
