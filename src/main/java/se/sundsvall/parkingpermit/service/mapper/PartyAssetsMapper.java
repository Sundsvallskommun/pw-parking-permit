package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.Status;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.util.ErrandUtil;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_STATUS;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_DESCRIPTION;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_ORIGIN;
import static se.sundsvall.parkingpermit.Constants.PARTY_ASSET_TYPE;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

public final class PartyAssetsMapper {

	private PartyAssetsMapper() {}

	public static AssetCreateRequest toAssetCreateRequest(Errand nullableErrand) {
		return ofNullable(nullableErrand)
			.map(errand -> new AssetCreateRequest()
				.addCaseReferenceIdsItem(Long.toString(errand.getId()))
				.assetId(getArtefactPermitNumber(errand))
				.description(PARTY_ASSET_DESCRIPTION)
				.issued(toIssued(errand))
				.origin(PARTY_ASSET_ORIGIN)
				.partyId(ErrandUtil.getStakeholder(errand, ROLE_APPLICANT).getPersonId())
				.status(toStatus(getArtefactPermitStatus(errand)))
				.type(PARTY_ASSET_TYPE)
				.validTo(toValidTo(errand)))
			.orElse(null);
	}

	private static String getArtefactPermitNumber(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(extraParameter.getKey()))
			.findFirst()
			.map(ExtraParameter::getValues)
			.filter(CollectionUtils::isNotEmpty)
			.map(List::getFirst)
			.orElse(null);
	}

	private static LocalDate toValidTo(Errand errand) {
		return ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.map(Decision::getValidTo)
			.map(OffsetDateTime::toLocalDate)
			.orElse(null);
	}

	private static LocalDate toIssued(Errand errand) {
		return ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.map(Decision::getValidFrom)
			.map(OffsetDateTime::toLocalDate)
			.orElse(null);
	}

	private static String getArtefactPermitStatus(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_STATUS.equals(extraParameter.getKey()))
			.findFirst()
			.map(ExtraParameter::getValues)
			.filter(CollectionUtils::isNotEmpty)
			.map(List::getFirst)
			.orElse(null);
	}

	private static Status toStatus(String nullableCaseDataStatus) {
		return ofNullable(nullableCaseDataStatus)
			.map(caseDataStatus -> switch (caseDataStatus)
			{
				case Constants.CASEDATA_PARKING_PERMIT_STATUS_ACTIVE -> Status.ACTIVE;
				case Constants.CASEDATA_PARKING_PERMIT_STATUS_BLOCKED -> Status.BLOCKED;
				case Constants.CASEDATA_PARKING_PERMIT_STATUS_EXPIRED -> Status.EXPIRED;
				default -> throw Problem.valueOf(CONFLICT, "Unexpected value: " + caseDataStatus);
			})
			.orElse(null);
	}
}
