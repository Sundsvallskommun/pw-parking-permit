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

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
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

		// Arrange
		final var municipalityId = "2281";
		final var caseNumber = 123L;
		final var uuid = randomUUID().toString();

		when(processServiceMock.startProcess(any(), any())).thenReturn(uuid);

		// Act
		final var response = webTestClient.post().uri(municipalityId + "/process/start/" + caseNumber)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(StartProcessResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response.getProcessId()).isEqualTo(uuid);
		verify(processServiceMock).startProcess(municipalityId, caseNumber);
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void updateProcess() {

		// Arrange
		final var municipalityId = "2281";
		final var uuid = randomUUID().toString();

		when(processServiceMock.startProcess(any(), any())).thenReturn(uuid);

		// Act
		webTestClient.post().uri(municipalityId + "/process/update/" + uuid)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert
		verify(processServiceMock).updateProcess(municipalityId, uuid);
		verifyNoMoreInteractions(processServiceMock);
	}
}
