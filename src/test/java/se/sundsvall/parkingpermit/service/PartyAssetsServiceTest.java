package se.sundsvall.parkingpermit.service;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.PROPOSED;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static generated.se.sundsvall.partyassets.Status.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_STATUS_ACTIVE;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.partyassets.Asset;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.AssetUpdateRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

@ExtendWith(MockitoExtension.class)
class PartyAssetsServiceTest {

	private static final String ERRAND_ID = "123";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PERMIT_NUMBER = "1234567890";
	private static final OffsetDateTime VALID_FROM = OffsetDateTime.now();
	private static final OffsetDateTime VALID_TO = VALID_FROM.plusDays(30);

	@Mock
	private PartyAssetsClient partyAssetsClientMock;

	@Mock
	private ResponseEntity<List<Asset>> responseEntityMock;

	@Captor
	private ArgumentCaptor<AssetCreateRequest> assetCreateRequestArgumentCaptor;

	@Captor
	private ArgumentCaptor<AssetUpdateRequest> assetUpdateRequestArgumentCaptor;

	@InjectMocks
	private PartyAssetsService partyAssetsService;

	@Test
	void createAsset() {

		// Arrange
		final var errand = createErrand();

		// Act
		partyAssetsService.createAsset(MUNICIPALITY_ID, errand);

		// Assert
		verify(partyAssetsClientMock).createAsset(eq(MUNICIPALITY_ID), assetCreateRequestArgumentCaptor.capture());

		final var assetCreateRequest = assetCreateRequestArgumentCaptor.getValue();
		assertThat(assetCreateRequest).isNotNull();
		assertThat(assetCreateRequest.getAssetId()).isEqualTo(PERMIT_NUMBER);
		assertThat(assetCreateRequest.getPartyId()).isEqualTo(getStakeholder(errand, ROLE_APPLICANT).getPersonId());
		assertThat(assetCreateRequest.getCaseReferenceIds()).containsExactly(Long.toString(errand.getId()));
		assertThat(assetCreateRequest.getType()).isEqualTo("PARKINGPERMIT");
		assertThat(assetCreateRequest.getIssued()).isEqualTo(VALID_FROM.toLocalDate());
		assertThat(assetCreateRequest.getValidTo()).isEqualTo(VALID_TO.toLocalDate());
		assertThat(assetCreateRequest.getStatus()).hasToString("ACTIVE");
		assertThat(assetCreateRequest.getDescription()).isEqualTo("Parkeringstillstånd");
	}

	@Test
	void getAssets() {
		// Arrange
		final var assetId = "assetId";
		final var partyId = "partyId";
		final var assets = List.of(new Asset().id("1").assetId(assetId).partyId(partyId).status(ACTIVE));

		when(partyAssetsClientMock.getAssets(MUNICIPALITY_ID, assetId, partyId, PARTY_ASSET_STATUS_ACTIVE)).thenReturn(responseEntityMock);
		when(responseEntityMock.getBody()).thenReturn(assets);

		// Act
		final var result = partyAssetsService.getAssets(MUNICIPALITY_ID, assetId, partyId, PARTY_ASSET_STATUS_ACTIVE);

		// Assert
		assertThat(result).isEqualTo(assets);
		verify(partyAssetsClientMock).getAssets(MUNICIPALITY_ID, assetId, partyId, PARTY_ASSET_STATUS_ACTIVE);
		verify(responseEntityMock).getBody();
	}

	@Test
	void getAssetsWhenNullInAssetId() {

		// Act
		final var result = partyAssetsService.getAssets(MUNICIPALITY_ID, null, "partyId", PARTY_ASSET_STATUS_ACTIVE);

		// Assert
		assertThat(result).isEmpty();
		verifyNoInteractions(partyAssetsClientMock, responseEntityMock);
	}

	@Test
	void getAssetsWhenNullInPartyId() {

		// Act
		final var result = partyAssetsService.getAssets(MUNICIPALITY_ID, "assetId", null, PARTY_ASSET_STATUS_ACTIVE);

		// Assert
		assertThat(result).isEmpty();
		verifyNoInteractions(partyAssetsClientMock, responseEntityMock);
	}

	@Test
	void updateAssetWithNewAdditionalParameter() {
		// Arrange
		final var asset = new Asset().id("1").assetId("assetId").partyId("partyId").status(ACTIVE);
		final var caseNumber = 123L;

		// Act
		partyAssetsService.updateAssetWithNewAdditionalParameter(MUNICIPALITY_ID, asset, caseNumber);

		// Assert
		verify(partyAssetsClientMock).updateAsset(eq(MUNICIPALITY_ID), eq(asset.getId()), assetUpdateRequestArgumentCaptor.capture());

		final var assetUpdateRequest = assetUpdateRequestArgumentCaptor.getValue();
		assertThat(assetUpdateRequest).isNotNull();
		assertThat(assetUpdateRequest.getAdditionalParameters()).hasSize(1);
		assertThat(assetUpdateRequest.getAdditionalParameters().get("appealedErrand")).isEqualTo(String.valueOf(caseNumber));
	}

	@Test
	void updateAssetWithNewAdditionalParameterWhenAssetIsNull() {
		// Act
		partyAssetsService.updateAssetWithNewAdditionalParameter(MUNICIPALITY_ID, null, 123L);

		// Assert
		verifyNoInteractions(partyAssetsClientMock);
	}

	@Test
	void updateAssetWithNewAdditionalParameterWhenCaseNumberIsNull() {
		// Act
		partyAssetsService.updateAssetWithNewAdditionalParameter(MUNICIPALITY_ID, new Asset(), null);

		// Assert
		verifyNoInteractions(partyAssetsClientMock);
	}

	private static Errand createErrand() {
		return new Errand()
			.id(Long.valueOf(ERRAND_ID))
			.decisions(List.of(createDecision(PROPOSED), createDecision(FINAL)))
			.stakeholders(List.of(createStakeholder(ROLE_APPLICANT), createStakeholder(ROLE_ADMINISTRATOR)))
			.extraParameters(List.of(
				new ExtraParameter("artefact.permit.number").addValuesItem(PERMIT_NUMBER),
				new ExtraParameter("artefact.permit.status").addValuesItem("Aktivt")));
	}

	public static Stakeholder createStakeholder(String role) {
		return new Stakeholder()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}

	public static Decision createDecision(Decision.DecisionTypeEnum decisionType) {
		return new Decision()
			.decisionType(decisionType)
			.validFrom(VALID_FROM)
			.validTo(VALID_TO);
	}
}
