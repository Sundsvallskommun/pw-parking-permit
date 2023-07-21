package se.sundsvall.parkingpermit.businesslogic.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;

import java.util.Map;
import java.util.UUID;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;

@ExtendWith(MockitoExtension.class)
class SendDenialDecisionTaskWorkerTest {
	private static final long ERRAND_ID = 123L;

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Mock
	private MessagingService messagingServiceMock;

	@Mock
	private ErrandDTO errandMock;

	@InjectMocks
	private SendDenialDecisionTaskWorker worker;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("SendDenialDecisionTask");
	}

	@Test
	void execute() {
		// Setup
		final var pdf = new RenderResponse();
		final var messageUUID = UUID.randomUUID();
		
		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(messagingServiceMock.renderPdf(errandMock)).thenReturn(pdf);
		when(messagingServiceMock.sendMessageToNonCitizen(errandMock, pdf)).thenReturn(messageUUID);
		
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(messagingServiceMock).renderPdf(errandMock);
		verify(messagingServiceMock).sendMessageToNonCitizen(errandMock, pdf);
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageUUID.toString()));
		verifyNoInteractions(camundaClientMock, failureHandlerMock);
	}

	@Test
	void executeThrowsException() {
		// Mock to throw exception when sending message to applicant
		final var pdf = new RenderResponse();

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(messagingServiceMock.renderPdf(errandMock)).thenReturn(pdf);
		when(messagingServiceMock.sendMessageToNonCitizen(errandMock, pdf)).thenThrow(Problem.valueOf(BAD_GATEWAY, "No message id received from messaging service"));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(messagingServiceMock).renderPdf(errandMock);
		verify(messagingServiceMock).sendMessageToNonCitizen(errandMock, pdf);
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verify(externalTaskServiceMock, never()).complete(any());
		verifyNoInteractions(camundaClientMock);
	}
}
