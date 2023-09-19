package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.citizenassets.AssetCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.citizenassets.CitizenAssetsClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static se.sundsvall.parkingpermit.util.ErrandUtil.getStakeholder;

@ExtendWith(MockitoExtension.class)
class CitizenAssetsServiceTest {

	final static String ERRAND_ID = "123";
	final static String PERMIT_NUMBER = "1234567890";
	final static OffsetDateTime UPDATED = OffsetDateTime.now();
	final static String EXPIRATION_DATE = "2023-12-31";

	@Mock
	private CitizenAssetsClient citizenAssetsClientMock;

	@Captor
	private ArgumentCaptor<AssetCreateRequest> assetCreateRequestArgumentCaptor;

	@InjectMocks
	private CitizenAssetsService citizenAssetsService;

	@Test
	void createAsset() {

		// Arrange
		final var errand = createErrand();

		// Act
		citizenAssetsService.createCitizenAsset(errand);

		// Assert
		verify(citizenAssetsClientMock).createAsset(assetCreateRequestArgumentCaptor.capture());
		final var assetCreateRequest = assetCreateRequestArgumentCaptor.getValue();
		assertThat(assetCreateRequest).isNotNull();
		assertThat(assetCreateRequest.getAssetId()).isEqualTo(PERMIT_NUMBER);
		assertThat(assetCreateRequest.getPartyId()).isEqualTo(getStakeholder(errand, APPLICANT).getPersonId());
		assertThat(assetCreateRequest.getCaseReferenceIds()).containsExactly(Long.toString(errand.getId()));
		assertThat(assetCreateRequest.getType()).isEqualTo("PARKINGPERMIT");
		assertThat(assetCreateRequest.getIssued()).isEqualTo(UPDATED.toLocalDate());
		assertThat(assetCreateRequest.getValidTo()).isEqualTo(LocalDate.parse(EXPIRATION_DATE));
		assertThat(assetCreateRequest.getStatus()).hasToString("ACTIVE");
		assertThat(assetCreateRequest.getDescription()).isEqualTo("Parkeringstillstånd");
	}

	private static ErrandDTO createErrand() {
		return new ErrandDTO()
			.id(Long.valueOf(ERRAND_ID))
			.updated(UPDATED)
			.stakeholders(List.of(createStakeholder(APPLICANT), createStakeholder(ADMINISTRATOR)))
			.extraParameters(Map.of("application.renewal.expirationDate", EXPIRATION_DATE,
									"artefact.permit.number", PERMIT_NUMBER,
									"artefact.permit.status", "Aktivt")
			);
	}

	public static StakeholderDTO createStakeholder(RolesEnum role) {
		return new StakeholderDTO()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}
}
