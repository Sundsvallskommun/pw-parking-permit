package se.sundsvall.parkingpermit.service;

import static se.sundsvall.parkingpermit.service.mapper.PartyAssetsMapper.toAssetCreateRequest;

import org.springframework.stereotype.Service;

import generated.se.sundsvall.casedata.ErrandDTO;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

@Service
public class PartyAssetsService {

	private final PartyAssetsClient partyAssetsClient;

	public PartyAssetsService(PartyAssetsClient partyAssetsClient) {
		this.partyAssetsClient = partyAssetsClient;
	}

	public void createAsset(String municipalityId, ErrandDTO errand) {
		partyAssetsClient.createAsset(municipalityId, toAssetCreateRequest(errand));
	}
}
