package se.sundsvall.parkingpermit.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.service.ProcessService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ProcessResourceFailuresTest {

	@MockBean
	private ProcessService processServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void startProcessInvalidCaseNumberIsNegative() {

		// Arrange
		final var caseNumber = -123L;

		// Act
		final var response = webTestClient.post().uri("/2281/process/start/" + caseNumber)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("startProcess.caseNumber", "must be greater than 0"));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void startProcessInvalidCaseNumberIsString() {

		// Arrange
		final var caseNumber = "invalid";

		// Act
		final var response = webTestClient.post().uri("/2281/process/start/" + caseNumber)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"invalid\"");

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void updateProcessInvalidProcessInstanceIdIsNotUUID() {

		// Arrange
		final var processInstanceId = "invalid";

		// Act
		final var response = webTestClient.post().uri("/2281/process/update/" + processInstanceId)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateProcess.processInstanceId", "not a valid UUID"));

		verifyNoInteractions(processServiceMock);
	}
}
