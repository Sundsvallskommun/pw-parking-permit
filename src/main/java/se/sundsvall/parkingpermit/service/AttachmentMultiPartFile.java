package se.sundsvall.parkingpermit.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static java.util.Objects.requireNonNull;

public class AttachmentMultiPartFile implements MultipartFile {

	private final InputStream contentStream;
	private final String fileName;

	private AttachmentMultiPartFile(final String fileName, final InputStream contentStream) {
		this.contentStream = contentStream;
		this.fileName = fileName;
	}

	public static AttachmentMultiPartFile create(final String fileName, final InputStream contentStream) {
		requireNonNull(fileName, "Filename must be provided");
		requireNonNull(contentStream, "Content stream must be provided");
		return new AttachmentMultiPartFile(fileName, contentStream);
	}

	@Override
	public String getName() {
		return fileName;
	}

	@Override
	public String getOriginalFilename() {
		return fileName;
	}

	@Override
	public String getContentType() {
		try {
			if (contentStream.available() == 0) {
				return "";
			}
			return MediaType.APPLICATION_PDF_VALUE;
		} catch (final IOException e) {
			return "";
		}
	}

	@Override
	public boolean isEmpty() {
		try {
			return contentStream.available() == 0;
		} catch (final IOException e) {
			return true;
		}
	}

	@Override
	public long getSize() {

		try {
			return contentStream.available();
		} catch (final IOException e) {
			return 0;
		}

	}

	@Override
	public byte[] getBytes() throws IOException {
		return contentStream.readAllBytes();
	}

	@Override
	public InputStream getInputStream() {
		return contentStream;
	}

	@Override
	public void transferTo(final File dest) throws IOException, IllegalStateException {
		try (final FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
			contentStream.transferTo(fileOutputStream);
		}
	}
}
