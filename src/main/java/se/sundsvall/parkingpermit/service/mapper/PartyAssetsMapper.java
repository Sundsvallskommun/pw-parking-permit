package se.sundsvall.parkingpermit.service.mapper;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.*;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import java.time.LocalDate;
import java.util.Optional;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.util.ErrandUtil;

public final class PartyAssetsMapper {

	private PartyAssetsMapper() {}

	public static AssetCreateRequest toAssetCreateRequest(Errand errand) {
		if (isNull(errand)) {
			return null;
		}
		return new AssetCreateRequest()
			.addCaseReferenceIdsItem(Long.toString(errand.getId()))
			.assetId(getArtefactPermitNumber(errand))
			.description(PARTY_ASSET_DESCRIPTION)
			.issued(toIssued(errand))
			.origin(PARTY_ASSET_ORIGIN)
			.partyId(ErrandUtil.getStakeholder(errand, ROLE_APPLICANT).getPersonId())
			.status(toStatus(getArtefactPermitStatus(errand)))
			.type(PARTY_ASSET_TYPE)
			.validTo(toValidTo(errand));
	}

	private static String getArtefactPermitNumber(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(extraParameter.getKey()))
			.findFirst()
			.map(extraParameter -> extraParameter.getValues().getFirst()).orElse(null);
	}

	private static LocalDate toValidTo(Errand errand) {
		final var validTo = Optional.ofNullable(errand.getDecisions()).orElse(emptyList())
			.stream().filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst().map(Decision::getValidTo).orElse(null);

		if (isNull(validTo)) {
			return null;
		}
		return validTo.toLocalDate();
	}

	private static LocalDate toIssued(Errand errand) {
		final var issued = Optional.ofNullable(errand.getDecisions()).orElse(emptyList())
			.stream().filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst().map(Decision::getValidFrom).orElse(null);

		if (isNull(issued)) {
			return null;
		}
		return issued.toLocalDate();
	}

	private static String getArtefactPermitStatus(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_STATUS.equals(extraParameter.getKey()))
			.findFirst()
			.map(extraParameter -> extraParameter.getValues().getFirst()).orElse(null);
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
