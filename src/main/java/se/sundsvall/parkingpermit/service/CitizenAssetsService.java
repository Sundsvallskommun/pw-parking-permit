package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.ErrandDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.integration.citizenassets.CitizenAssetsClient;

import static se.sundsvall.parkingpermit.service.mapper.CitizenAssetsMapper.toAssetCreateRequest;

@Service
public class CitizenAssetsService {

	@Autowired
	private CitizenAssetsClient citizenAssetsClient;

	public void createCitizenAsset(ErrandDTO errand) {

		citizenAssetsClient.createAsset(toAssetCreateRequest(errand));
	}
}
