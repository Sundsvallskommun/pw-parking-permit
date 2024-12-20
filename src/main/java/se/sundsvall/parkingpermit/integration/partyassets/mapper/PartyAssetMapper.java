package se.sundsvall.parkingpermit.integration.partyassets.mapper;

import static java.util.Objects.isNull;

import generated.se.sundsvall.partyassets.Asset;
import generated.se.sundsvall.partyassets.AssetUpdateRequest;
import java.util.HashMap;
import java.util.Optional;

public class PartyAssetMapper {

	private PartyAssetMapper() {}

	public static AssetUpdateRequest toAssetUpdateRequest(Asset asset, Long caseNumber) {
		if (isNull(asset) || isNull(caseNumber)) {
			return null;
		}

		final var assetUpdateRequest = new AssetUpdateRequest();
		assetUpdateRequest.additionalParameters(Optional.ofNullable(asset.getAdditionalParameters()).orElse(new HashMap<>()));
		assetUpdateRequest.putAdditionalParametersItem("appealedErrand", String.valueOf(caseNumber));

		return assetUpdateRequest;
	}
}
