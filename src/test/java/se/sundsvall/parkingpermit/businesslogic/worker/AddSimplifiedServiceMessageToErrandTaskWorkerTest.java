package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.MessageRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID;

@ExtendWith(MockitoExtension.class)
class AddSimplifiedServiceMessageToErrandTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

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
	private TextProvider textProviderMock;

	@Mock
	private SimplifiedServiceTextProperties simplifiedServiceTextPropertiesMock;

	@Mock
	private Errand errandMock;

	@InjectMocks
	private AddSimplifiedServiceMessageToErrandTaskWorker worker;

	@Captor
	private ArgumentCaptor<MessageRequest> messageRequestCaptor;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("AddSimplifiedServiceMessageToErrandTask");
	}

	@Test
	void execute() {
		final var externalCaseId = "externalCaseId";
		final var subject = "subject";
		final var plainBody = "plainBody";
		final var messageId = UUID.randomUUID().toString();

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.<String>getVariable(CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID)).thenReturn(messageId);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(simplifiedServiceTextPropertiesMock.getSubject()).thenReturn(subject);
		when(simplifiedServiceTextPropertiesMock.getPlainBody()).thenReturn(plainBody);
		when(errandMock.getExternalCaseId()).thenReturn(externalCaseId);

		worker.execute(externalTaskMock, externalTaskServiceMock);

		verify(caseDataClientMock).addMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), messageRequestCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		final var captured = messageRequestCaptor.getValue();
		assertThat(captured.getDirection()).isEqualTo(OUTBOUND);
		assertThat(captured.getMessageId()).isEqualTo(messageId);
		assertThat(captured.getSubject()).isEqualTo(subject);
		assertThat(captured.getMessage()).isEqualTo(plainBody);
		assertThat(captured.getExternalCaseId()).isEqualTo(externalCaseId);
		assertThat(captured.getAttachments()).isNull();
		assertThat(OffsetDateTime.parse(captured.getSent())).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void executeWhenMessageIdMissingCompletesWithoutAddingMessage() {
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.<String>getVariable(CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID)).thenReturn(null);

		worker.execute(externalTaskMock, externalTaskServiceMock);

		verify(externalTaskServiceMock).complete(externalTaskMock);
		verify(caseDataClientMock, never()).addMessage(any(), any(), any(), any());
		verifyNoInteractions(failureHandlerMock, camundaClientMock, textProviderMock);
	}

	@Test
	void executeHandlesException() {
		final var messageId = UUID.randomUUID().toString();
		final var failureMessage = "boom";

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.<String>getVariable(CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID)).thenReturn(messageId);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(new RuntimeException(failureMessage));

		worker.execute(externalTaskMock, externalTaskServiceMock);

		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, failureMessage);
		verify(caseDataClientMock, never()).addMessage(any(), any(), any(), any());
		verify(externalTaskServiceMock, never()).complete(any());
	}
}
