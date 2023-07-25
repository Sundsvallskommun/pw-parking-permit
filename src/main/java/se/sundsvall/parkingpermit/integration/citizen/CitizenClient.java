package se.sundsvall.parkingpermit.integration.citizen;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.parkingpermit.integration.citizen.configuration.CitizenConfiguration.CLIENT_ID;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import generated.se.sundsvall.citizen.CitizenExtended;
import se.sundsvall.parkingpermit.integration.citizen.configuration.CitizenConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.citizen.url}", configuration = CitizenConfiguration.class)
public interface CitizenClient {

	/**
	 * Method for retrieving a citizen
	 *
	 * @param  personId                             the person ID
	 * @return                                      object with citizen data.
	 * @throws org.zalando.problem.ThrowableProblem when called service responds with error code
	 */
	@GetMapping(path = "/{personId}", produces = TEXT_PLAIN_VALUE)
	Optional<CitizenExtended> getCitizen(@PathVariable("personId") String personId);
}
