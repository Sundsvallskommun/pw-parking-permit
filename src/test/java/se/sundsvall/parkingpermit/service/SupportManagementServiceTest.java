package se.sundsvall.parkingpermit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.supportmanagement.Errand;
import generated.se.sundsvall.supportmanagement.Label;
import generated.se.sundsvall.supportmanagement.Labels;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.parkingpermit.integration.supportmanagement.SupportManagementClient;

@ExtendWith(MockitoExtension.class)
class SupportManagementServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private SupportManagementClient supportManagementClientMock;

	@InjectMocks
	private SupportManagementService supportManagementService;

	@Test
	void createErrand() {
		// Arrange
		final var errand = new Errand();

		when(supportManagementClientMock.createErrand(MUNICIPALITY_ID, NAMESPACE, errand)).thenReturn(ResponseEntity.created(URI.create("http://localhost:8080/errands/errandId")).build());

		// Act
		supportManagementService.createErrand(MUNICIPALITY_ID, NAMESPACE, errand);

		// Assert
		verify(supportManagementClientMock).createErrand(MUNICIPALITY_ID, NAMESPACE, errand);
	}

	@Test
	void errorWhenCreatingErrand() {
		// Arrange
		final var errand = new Errand();
		when(supportManagementClientMock.createErrand(MUNICIPALITY_ID, NAMESPACE, errand)).thenReturn(ResponseEntity.badRequest().build());
		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> supportManagementService.createErrand(MUNICIPALITY_ID, NAMESPACE, errand));
		// Assert
		verify(supportManagementClientMock).createErrand(MUNICIPALITY_ID, NAMESPACE, errand);
		assertThat(exception.getStatus()).isEqualTo(BAD_GATEWAY);
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: Failed to create errand in support-management");
	}

	@Test
	void errorWhenExtractingLocationFromResponse() {
		// Arrange
		final var errand = new Errand();
		when(supportManagementClientMock.createErrand(MUNICIPALITY_ID, NAMESPACE, errand)).thenReturn(ResponseEntity.created(URI.create("invalid-uri")).build());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> supportManagementService.createErrand(MUNICIPALITY_ID, NAMESPACE, errand));

		// Assert
		verify(supportManagementClientMock).createErrand(MUNICIPALITY_ID, NAMESPACE, errand);
		assertThat(exception.getStatus()).isEqualTo(BAD_GATEWAY);
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: Invalid location header in response from support-management");
	}

	@Test
	void createAttachment() {
		// Arrange
		final var errandId = "errandId";
		final var fileName = "file.txt";
		final var content = Base64.getEncoder().encodeToString("file content".getBytes());

		// Act
		supportManagementService.createAttachment(MUNICIPALITY_ID, NAMESPACE, errandId, fileName, content);

		// Assert
		verify(supportManagementClientMock).createAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(errandId), any(AttachmentMultiPartFile.class));
	}

	@Test
	void createAttachmentNoFilename() {
		// Arrange
		final var errandId = "errandId";
		final var content = "file content";

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> supportManagementService.createAttachment(MUNICIPALITY_ID, NAMESPACE, errandId, null, content));

		// Assert
		assertThat(exception.getStatus()).isEqualTo(BAD_GATEWAY);
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: File name and content cannot be null or empty");
		verifyNoInteractions(supportManagementClientMock);
	}

	@Test
	void getMetadataLabels() {
		// Arrange
		var label = new Label().id("x");
		when(supportManagementClientMock.getLabels(any(), any())).thenReturn(ResponseEntity.ok().body(new Labels().labelStructure(List.of(label))));

		// Act

		var result = supportManagementService.getMetadataLabels(MUNICIPALITY_ID, NAMESPACE);

		// Assert
		assertThat(result).hasSize(1).containsExactly(label);
		verify(supportManagementClientMock).getLabels(MUNICIPALITY_ID, NAMESPACE);
	}

	@Test
	void getMetadataLabelsError() {
		// Arrange
		when(supportManagementClientMock.getLabels(any(), any())).thenReturn(ResponseEntity.badRequest().build());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> supportManagementService.getMetadataLabels(MUNICIPALITY_ID, NAMESPACE));

		// Assert
		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getMessage()).isEqualTo("Internal Server Error: Failed to get metadata labels from support-management");
		verify(supportManagementClientMock).getLabels(MUNICIPALITY_ID, NAMESPACE);
	}
}
