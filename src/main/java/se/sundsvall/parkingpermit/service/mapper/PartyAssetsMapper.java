package se.sundsvall.parkingpermit.service.mapper;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_RENEWAL_EXPERATION_DATE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_DESCRIPTION;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_ORIGIN;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_TYPE;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.zalando.problem.Problem;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.util.ErrandUtil;

public class PartyAssetsMapper {

	private PartyAssetsMapper() {
	}

	public static AssetCreateRequest toAssetCreateRequest(ErrandDTO errandDTO) {
		if (isNull(errandDTO)) {
			return null;
		}
		return new AssetCreateRequest()
			.addCaseReferenceIdsItem(Long.toString(errandDTO.getId()))
			.assetId(getArtefactPermitNumber(errandDTO))
			.description(PARTY_ASSET_DESCRIPTION)
			.issued(toLocalDate(errandDTO.getUpdated()))
			.origin(PARTY_ASSET_ORIGIN)
			.partyId(ErrandUtil.getStakeholder(errandDTO, APPLICANT).getPersonId())
			.status(toStatus(getArtefactPermitStatus(errandDTO)))
			.type(PARTY_ASSET_TYPE)
			.validTo(toValidTo(errandDTO));
	}

	private static String getArtefactPermitNumber(ErrandDTO errandDTO) {
		return ofNullable(errandDTO.getExtraParameters()).orElse(emptyMap()).get(CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER);
	}

	private static LocalDate toLocalDate(OffsetDateTime offsetDateTime) {
		if (isNull(offsetDateTime)) {
			return null;
		}

		return offsetDateTime.toLocalDate();
	}

	private static LocalDate toValidTo(ErrandDTO errandDTO) {
		final var validTo = Optional.ofNullable(errandDTO.getExtraParameters()).orElse(emptyMap()).get(CASEDATA_KEY_APPLICATION_RENEWAL_EXPERATION_DATE);

		if (isNull(validTo)) {
			return null;
		}
		return LocalDate.parse(validTo);
	}

	private static String getArtefactPermitStatus(ErrandDTO errandDTO) {
		return ofNullable(errandDTO.getExtraParameters()).orElse(emptyMap()).get(CASEDATA_KEY_ARTEFACT_PERMIT_STATUS);
	}

	private static Status toStatus(String caseDataStatus) {
		if (isNull(caseDataStatus)) {
			return null;
		}

		return switch (caseDataStatus) {
			case Constants.CASEDATA_PARKING_PERMIT_STATUS_ACTIVE -> Status.ACTIVE;
			case Constants.CASEDATA_PARKING_PERMIT_STATUS_BLOCKED -> Status.BLOCKED;
			case Constants.CASEDATA_PARKING_PERMIT_STATUS_EXPIRED -> Status.EXPIRED;
			default -> throw Problem.valueOf(CONFLICT, "Unexpected value: " + caseDataStatus);
		};
	}
}
