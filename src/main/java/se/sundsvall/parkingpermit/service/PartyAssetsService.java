package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.Errand;
import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

import static se.sundsvall.parkingpermit.service.mapper.PartyAssetsMapper.toAssetCreateRequest;

@Service
public class PartyAssetsService {

	private final PartyAssetsClient partyAssetsClient;

	public PartyAssetsService(PartyAssetsClient partyAssetsClient) {
		this.partyAssetsClient = partyAssetsClient;
	}

	public void createAsset(String municipalityId, Errand errand) {
		partyAssetsClient.createAsset(municipalityId, toAssetCreateRequest(errand));
	}
}
