package se.sundsvall.parkingpermit.integration.partyassets;

import generated.se.sundsvall.partyassets.Asset;
import generated.se.sundsvall.partyassets.AssetCreateRequest;
import generated.se.sundsvall.partyassets.AssetUpdateRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.parkingpermit.integration.partyassets.configuration.PartyAssetsConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.partyassets.configuration.PartyAssetsConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.partyassets.url}", configuration = PartyAssetsConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface PartyAssetsClient {

	/**
	 * Create asset for party.
	 *
	 * @param municipalityId     the municipalityId.
	 * @param assetCreateRequest request containing asset information.
	 */
	@PostMapping(path = "/{municipalityId}/assets", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createAsset(@PathVariable final String municipalityId, final @RequestBody AssetCreateRequest assetCreateRequest);

	/**
	 * Get asset for party.
	 *
	 * @param municipalityId the municipalityId.
	 * @param assetId        the assetId.
	 * @param partyId        the partyId of applicant.
	 * @param status         the status of assets.
	 */
	@GetMapping(path = "/{municipalityId}/assets", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<Asset>> getAssets(@PathVariable final String municipalityId, @RequestParam final String assetId,
		@RequestParam final String partyId, @RequestParam final String status);

	/**
	 * Get asset for party.
	 *
	 * @param municipalityId the municipalityId.
	 * @param partyId        the partyId of applicant.
	 * @param status         the status of assets.
	 */
	@GetMapping(path = "/{municipalityId}/assets", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<Asset>> getAssets(@PathVariable final String municipalityId,
		@RequestParam final String partyId, @RequestParam final String status);

	/**
	 * Update asset.
	 *
	 * @param municipalityId     the municipalityId.
	 * @param id                 the assetId.
	 * @param assetUpdateRequest request containing asset information.
	 */
	@PutMapping(path = "/{municipalityId}/assets/{id}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> updateAsset(@PathVariable final String municipalityId, @PathVariable final String id,
		final @RequestBody AssetUpdateRequest assetUpdateRequest);
}
