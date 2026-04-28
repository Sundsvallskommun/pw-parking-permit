package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.partyassets.Asset;
import generated.se.sundsvall.partyassets.AssetUpdateRequest;
import generated.se.sundsvall.partyassets.Status;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.support.Relation;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static se.sundsvall.parkingpermit.service.mapper.PartyAssetsMapper.toAssetCreateRequest;

@Service
public class PartyAssetsService {

	private static final String RELATION_TYPE_LINK = "LINK";
	private static final String RELATION_IDENTIFIER_TYPE_CASE = "case";
	private static final String RELATION_IDENTIFIER_SERVICE_CASE_DATA = "casedata";

	private final PartyAssetsClient partyAssetsClient;

	public PartyAssetsService(PartyAssetsClient partyAssetsClient) {
		this.partyAssetsClient = partyAssetsClient;
	}

	public void createAsset(String municipalityId, String namespace, Errand errand) {
		partyAssetsClient.createAsset(municipalityId, toSourceReference(namespace, errand.getId()), toAssetCreateRequest(errand));
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

	public void updateAssetWithNewStatus(String municipalityId, String id, Status status, String statusReason) {
		if (isNull(id) || isNull(status)) {
			return;
		}
		partyAssetsClient.updateAsset(municipalityId, id, new AssetUpdateRequest().status(status).statusReason(statusReason));
	}

	public String toSourceReference(String namespace, Long caseNumber) {
		if (isNull(namespace) || isNull(caseNumber)) {
			return null;
		}
		return Relation.create(RELATION_TYPE_LINK, Relation.ResourceIdentifier.create(caseNumber.toString(), RELATION_IDENTIFIER_TYPE_CASE, RELATION_IDENTIFIER_SERVICE_CASE_DATA, namespace), null)
			.toRelationString();
	}
}
