package se.sundsvall.parkingpermit.integration.partyassets;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.partyassets.configuration.PartyAssetsConfiguration.CLIENT_ID;

import generated.se.sundsvall.partyassets.AssetCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.parkingpermit.integration.partyassets.configuration.PartyAssetsConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.partyassets.url}", configuration = PartyAssetsConfiguration.class)
public interface PartyAssetsClient {

	/**
	 * Create asset for party.
	 *
	 * @param municipalityId     the municipalityId.
	 * @param assetCreateRequest request containing asset information.
	 */
	@PostMapping(path = "/{municipalityId}/assets", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> createAsset(@PathVariable("municipalityId") String municipalityId, AssetCreateRequest assetCreateRequest);
}
