package se.sundsvall.parkingpermit.util;

import java.util.Objects;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

public final class LocationUtil {

	private LocationUtil() {}

	/**
	 * Extracts the numerical id of a created resource from the Location header of the response.
	 *
	 * @param  response     the response of the create request
	 * @param  resourceName name of the created resource, used in the error message when no id can be extracted
	 * @return              the id of the created resource
	 */
	public static Long extractIdFromLocation(final ResponseEntity<Void> response, final String resourceName) {
		return ofNullable(response)
			.map(ResponseEntity::getHeaders)
			.map(headers -> headers.get(LOCATION))
			.orElse(emptyList())
			.stream()
			.filter(Objects::nonNull)
			.map(locationValue -> locationValue.substring(locationValue.lastIndexOf('/') + 1))
			.filter(NumberUtils::isCreatable)
			.map(Long::valueOf)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "CaseData integration did not return any location for created %s".formatted(resourceName)));
	}
}
