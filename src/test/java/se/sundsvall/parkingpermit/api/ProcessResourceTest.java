package se.sundsvall.parkingpermit.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;
import se.sundsvall.parkingpermit.service.ProcessService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class ProcessResourceTest {

	@MockBean
	private ProcessService processServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void startProcess() {
		// Setup
		final var businessKey = "businessKey";
		final var uuid = UUID.randomUUID().toString();

		// Mock
		when(processServiceMock.startProcess(any())).thenReturn(uuid);

		// Act
		final var response = webTestClient.post().uri("/process/start/" + businessKey)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(StartProcessResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert and verify
		assertThat(response.getProcessId()).isEqualTo(uuid);
		verify(processServiceMock).startProcess(businessKey);
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void updateProcess() {
		// Setup
		final var uuid = UUID.randomUUID().toString();

		// Mock
		when(processServiceMock.startProcess(any())).thenReturn(uuid);

		// Act
		webTestClient.post().uri("/process/update/" + uuid)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert and verify
		verify(processServiceMock).updateProcess(uuid);
		verifyNoMoreInteractions(processServiceMock);
	}
}
