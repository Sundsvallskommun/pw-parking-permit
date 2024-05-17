package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.util.ErrandUtil;

import java.time.LocalDate;
import java.util.Optional;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_DESCRIPTION;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_ORIGIN;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_TYPE;

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
			.issued(toIssued(errandDTO))
			.origin(PARTY_ASSET_ORIGIN)
			.partyId(ErrandUtil.getStakeholder(errandDTO, APPLICANT).getPersonId())
			.status(toStatus(getArtefactPermitStatus(errandDTO)))
			.type(PARTY_ASSET_TYPE)
			.validTo(toValidTo(errandDTO));
	}

	private static String getArtefactPermitNumber(ErrandDTO errandDTO) {
		return ofNullable(errandDTO.getExtraParameters()).orElse(emptyMap()).get(CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER);
	}

	private static LocalDate toValidTo(ErrandDTO errandDTO) {
		final var validTo = Optional.ofNullable(errandDTO.getDecisions()).orElse(emptyList())
			.stream().filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst().map(DecisionDTO::getValidTo).orElse(null);

		if (isNull(validTo)) {
			return null;
		}
		return validTo.toLocalDate();
	}

	private static LocalDate toIssued(ErrandDTO errandDTO) {
		final var issued = Optional.ofNullable(errandDTO.getDecisions()).orElse(emptyList())
			.stream().filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst().map(DecisionDTO::getValidFrom).orElse(null);

		if (isNull(issued)) {
			return null;
		}
		return issued.toLocalDate();
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
