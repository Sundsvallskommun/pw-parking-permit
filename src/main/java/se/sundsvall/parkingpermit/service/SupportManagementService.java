package se.sundsvall.parkingpermit.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.supportmanagement.Errand;
import generated.se.sundsvall.supportmanagement.Label;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.integration.supportmanagement.SupportManagementClient;

@Service
public class SupportManagementService {

	private final SupportManagementClient supportManagementClient;

	SupportManagementService(final SupportManagementClient supportManagementClient) {
		this.supportManagementClient = supportManagementClient;
	}

	public Optional<String> createErrand(final String municipalityId, final String namespace, final Errand errand) {
		final var response = supportManagementClient.createErrand(municipalityId, namespace, errand);

		if (response.getStatusCode().is2xxSuccessful()) {
			return Optional.of(extractErrandIdFromLocation(response));
		} else {
			throw Problem.valueOf(BAD_GATEWAY, "Failed to create errand in support-management");
		}
	}

	public void createAttachment(final String municipalityId, final String namespace, final String errandId, final String fileName, final String content) {
		if (isNotEmpty(fileName) && Objects.nonNull(content)) {
			supportManagementClient.createAttachment(municipalityId, namespace, errandId,
				AttachmentMultiPartFile.create(fileName, new ByteArrayInputStream(Base64.getDecoder().decode(content.getBytes()))));
		} else {
			throw Problem.valueOf(BAD_GATEWAY, "File name and content cannot be null or empty");
		}
	}

	public List<Label> getMetadataLabels(final String municipalityId, final String namespace) {
		final var response = supportManagementClient.getLabels(municipalityId, namespace);

		if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
			return response.getBody().getLabelStructure();
		} else {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to get metadata labels from support-management");
		}
	}

	private String extractErrandIdFromLocation(ResponseEntity<Void> response) {
		final var location = String.valueOf(response.getHeaders().getFirst(LOCATION));
		if (location == null || !location.contains("/errands/")) {
			throw Problem.valueOf(BAD_GATEWAY, "Invalid location header in response from support-management");
		}
		return location.substring(location.lastIndexOf("/") + 1);
	}
}
