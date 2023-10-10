package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.ErrandDTO;
import se.sundsvall.parkingpermit.integration.partyassets.PartyAssetsClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static se.sundsvall.parkingpermit.service.mapper.PartyAssetsMapper.toAssetCreateRequest;

@Service
public class PartyAssetsService {

	@Autowired
	private PartyAssetsClient partyAssetsClient;

	public void createAsset(ErrandDTO errand) {

		partyAssetsClient.createAsset(toAssetCreateRequest(errand));
	}
}
