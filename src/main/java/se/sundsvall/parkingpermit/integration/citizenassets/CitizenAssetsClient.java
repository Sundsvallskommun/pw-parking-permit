package se.sundsvall.parkingpermit.integration.citizenassets;

import generated.se.sundsvall.citizenassets.AssetCreateRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.parkingpermit.integration.citizenassets.configuration.CitizenAssetsConfiguration;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.integration.citizenassets.configuration.CitizenAssetsConfiguration.CLIENT_ID;

@CircuitBreaker(name = CLIENT_ID)
@FeignClient(name = CLIENT_ID, url = "${integration.citizenassets.url}", configuration = CitizenAssetsConfiguration.class)
public interface CitizenAssetsClient {

	/**
	 * Create asset for citizen.
	 * @param assetCreateRequest request containing asset information
	 */
	@PostMapping(path = "/assets", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> createAsset(AssetCreateRequest assetCreateRequest);
}
