package se.sundsvall.parkingpermit.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wiremock.org.apache.commons.io.FileUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@ExtendWith(MockitoExtension.class)
class AttachmentMultiPartFileTest {

	@Mock
	private File fileMock;

	@Mock
	private InputStream inputStreamMock;

	@Test
	void fromContent() throws Exception {
		// Arrange
		final var fileName = "fileName";
		final var content = "content".getBytes();

		final var stream = new ByteArrayInputStream(content);

		// Act
		final var multipartFile = AttachmentMultiPartFile.create(fileName, stream);

		// Assert
		assertThat(multipartFile.getContentType()).isEqualTo(APPLICATION_PDF_VALUE);
		assertThat(multipartFile.getBytes()).isEqualTo(content);
		assertThat(multipartFile.getInputStream()).isEqualTo(stream);
		assertThat(multipartFile.getName()).isEqualTo(fileName);
		assertThat(multipartFile.getOriginalFilename()).isEqualTo(fileName);
		assertThat(multipartFile.getSize()).isZero();
		assertThat(multipartFile.isEmpty()).isTrue();
	}

	@Test
	void getSizeThrowsIOException() throws Exception {
		// Arrange
		final var fileName = "fileName";

		when(inputStreamMock.available()).thenThrow(new IOException("Test IOException"));

		final var multipartFile = AttachmentMultiPartFile.create(fileName, inputStreamMock);

		// Act & Assert
		assertThat(multipartFile.getSize()).isZero();
	}

	@Test
	void isEmptyThrowsIOException() throws Exception {
		// Arrange
		final var fileName = "fileName";

		when(inputStreamMock.available()).thenThrow(new IOException("Test IOException"));

		final var multipartFile = AttachmentMultiPartFile.create(fileName, inputStreamMock);

		// Act & Assert
		assertThat(multipartFile.isEmpty()).isTrue();
	}

	@Test
	void fromEmptyContent() throws Exception {

		// Arrange
		final var fileName = "fileName";
		final var content = "";
		final var stream = new ByteArrayInputStream(content.getBytes());

		// Act
		final var multipartFile = AttachmentMultiPartFile.create(fileName, stream);

		// Assert
		assertThat(multipartFile.getBytes()).isNullOrEmpty();
		assertThat(multipartFile.getContentType()).isEmpty();
		assertThat(multipartFile.getInputStream().readAllBytes()).isEmpty();
		assertThat(multipartFile.getName()).isEqualTo(fileName);
		assertThat(multipartFile.getOriginalFilename()).isEqualTo(fileName);
		assertThat(multipartFile.getSize()).isZero();
		assertThat(multipartFile.isEmpty()).isTrue();
	}

	@Test
	void fromAttachmentWithoutContent() {
		// Arrange
		final var fileName = "fileName";

		// Act & Assert
		assertThrows(NullPointerException.class, () -> AttachmentMultiPartFile.create(fileName, null), "Content must be provided");
	}

	@Test
	void transferToForAttachmentWithContent() throws Exception {
		// Arrange
		final var fileName = "fileName";
		final var content = "content".getBytes();
		final var stream = new ByteArrayInputStream(content);
		final var multipartFile = AttachmentMultiPartFile.create(fileName, stream);
		final var file = Files.createTempFile("test_", null).toFile();

		// Act
		multipartFile.transferTo(file);

		// Assert
		assertThat(file).exists();
		assertThat(FileUtils.readFileToByteArray(file)).isEqualTo(content);
	}

	@Test
	void transferToForAttachmentWithoutContent() {

		// Arrange
		final var fileName = "fileName";

		// Act & Assert
		assertThrows(NullPointerException.class, () -> AttachmentMultiPartFile.create(fileName, null), "Content must be provided");
		verifyNoInteractions(fileMock);
	}
}
