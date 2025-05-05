package se.sundsvall.parkingpermit.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static se.sundsvall.parkingpermit.integration.partyassets.mapper.PartyAssetMapper.toAssetUpdateRequest;
import static se.sundsvall.parkingpermit.service.mapper.PartyAssetsMapper.toAssetCreateRequest;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.partyassets.Asset;
import generated.se.sundsvall.partyassets.AssetUpdateRequest;
import generated.se.sundsvall.partyassets.Status;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

@Service
public class PartyAssetsService {

	private final PartyAssetsClient partyAssetsClient;

	public PartyAssetsService(PartyAssetsClient partyAssetsClient) {
		this.partyAssetsClient = partyAssetsClient;
	}

	public void createAsset(String municipalityId, Errand errand) {
		partyAssetsClient.createAsset(municipalityId, toAssetCreateRequest(errand));
	}

	public List<Asset> getAssets(String municipalityId, String assetId, String partyId, String status) {
		if (isNull(assetId) || isNull(partyId)) {
			return emptyList();
		}
		return partyAssetsClient.getAssets(municipalityId, assetId, partyId, status).getBody();
	}

	public List<Asset> getAssets(String municipalityId, String partyId, String status) {
		if (isNull(partyId)) {
			return emptyList();
		}
		return partyAssetsClient.getAssets(municipalityId, partyId, status).getBody();
	}

	public void updateAssetWithNewAdditionalParameter(String municipalityId, Asset asset, Long caseNumber) {
		if (isNull(asset) || isNull(caseNumber)) {
			return;
		}
		partyAssetsClient.updateAsset(municipalityId, asset.getId(), toAssetUpdateRequest(asset, caseNumber));
	}

	public void updateAssetWithNewStatus(String municipalityId, String id, Status status, String statusReason) {
		if (isNull(id) || isNull(status)) {
			return;
		}
		partyAssetsClient.updateAsset(municipalityId, id, new AssetUpdateRequest().status(status).statusReason(statusReason));
	}
}
