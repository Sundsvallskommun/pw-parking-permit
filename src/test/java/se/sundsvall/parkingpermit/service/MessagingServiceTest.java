package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.WebMessageRequest;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.parkingpermit.integration.messaging.MessagingClient;
import se.sundsvall.parkingpermit.integration.messaging.mapper.MessagingMapper;
import se.sundsvall.parkingpermit.integration.templating.TemplatingClient;
import se.sundsvall.parkingpermit.util.CommonTextProperties;
import se.sundsvall.parkingpermit.util.TextProperties;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ORGANIZATION_NUMBER = "1122334455";
	private static final String TEMPLATE_ID = "sbk.prh.decision.all.rejection.municipality";

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private TemplatingClient templatingClientMock;

	@Mock
	private RenderResponse renderResponseMock;

	@Mock
	private MessagingMapper messagingMapperMock;

	@Mock
	private TextProperties textPropertiesMock;

	@Mock
	private CommonTextProperties commonTextPropertiesMock;

	@InjectMocks
	private MessagingService messagingService;

	@Captor
	private ArgumentCaptor<LetterRequest> letterRequestCaptor;

	@Captor
	private ArgumentCaptor<WebMessageRequest> webMessageRequestCaptor;

	@Test
	void renderPdfNonCitizen() {

		// Arrange
		final var errand = createErrand(true);
		when(templatingClientMock.renderPdf(eq(MUNICIPALITY_ID), any())).thenReturn(renderResponseMock);

		// Act
		messagingService.renderPdfDecision(MUNICIPALITY_ID, errand, TEMPLATE_ID);

		// Assert
		verify(templatingClientMock).renderPdf(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(messagingClientMock, messagingMapperMock);
	}

	@Test
	void renderPdfDecision() {

		// Arrange
		final var errand = createErrand(true);
		final var templateIdentifier = "some-template-identifier";
		when(templatingClientMock.renderPdf(eq(MUNICIPALITY_ID), any())).thenReturn(renderResponseMock);

		// Act
		messagingService.renderPdfDecision(MUNICIPALITY_ID, errand, templateIdentifier);

		// Assert
		verify(templatingClientMock).renderPdf(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(messagingClientMock, messagingMapperMock);
	}

	@Test
	void sendMessageToNonCitizenWithExternalCaseIdPresentInErrand() {

		// Arrange
		final var errand = createErrand(true);
		final var renderResponse = new RenderResponse();
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingMapperMock.toWebMessageRequestDenial(any(), any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var uuid = messagingService.sendMessageToNonCitizen(MUNICIPALITY_ID, errand, renderResponse);

		// Assert
		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void sendMessageToNonCitizenWithExternalCaseIdAbsent() {

		// Arrange
		final var errand = createErrand(false);
		final var renderResponse = new RenderResponse();
		final var letterRequest = new LetterRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());
		final var messageBatchResult = new MessageBatchResult().addMessagesItem(messageResult);

		when(messagingMapperMock.toLetterRequestDenial(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(letterRequest);
		when(messagingClientMock.sendLetter(eq(MUNICIPALITY_ID), any())).thenReturn(messageBatchResult);

		// Act
		final var uuid = messagingService.sendMessageToNonCitizen(MUNICIPALITY_ID, errand, renderResponse);

		// Assert
		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendLetter(MUNICIPALITY_ID, letterRequest);
		verify(messagingClientMock, never()).sendWebMessage(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingWebmessageResource() {

		// Arrange
		final var errand = createErrand(true);
		final var renderResponse = new RenderResponse();
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult();

		when(messagingMapperMock.toWebMessageRequestDenial(any(), any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendMessageToNonCitizen(MUNICIPALITY_ID, errand, renderResponse));

		// Assert
		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingLetterResource() {

		// Arrange
		final var errand = createErrand(false);
		final var renderResponse = new RenderResponse();
		final var letterRequest = new LetterRequest();
		final var messageBatchResult = new MessageBatchResult();

		when(messagingMapperMock.toLetterRequestDenial(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(letterRequest);
		when(messagingClientMock.sendLetter(eq(MUNICIPALITY_ID), any())).thenReturn(messageBatchResult);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendMessageToNonCitizen(MUNICIPALITY_ID, errand, renderResponse));

		// Assert
		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingClientMock).sendLetter(MUNICIPALITY_ID, letterRequest);
		verify(messagingClientMock, never()).sendWebMessage(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void sendMessageSimplifiedServiceWithExternalCaseIdPresentInErrand() {

		// Arrange
		final var errand = createErrand(true);
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingMapperMock.toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var uuid = messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand);

		// Assert
		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingWebmessageResourceSimplifiedService() {

		// Arrange
		final var errand = createErrand(true);
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult();

		when(messagingMapperMock.toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var messageId = messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand);

		// Assert
		assertThat(messageId).isNull();
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingDigitalMailResourceSimplifiedService() {

		// Arrange
		final var errand = createErrand(false);
		final var digitalMailRequest = new DigitalMailRequest();
		final var messageBatchResult = new MessageBatchResult();

		when(textPropertiesMock.getCommons()).thenReturn(Map.of(MUNICIPALITY_ID, commonTextPropertiesMock));
		when(commonTextPropertiesMock.getOrganizationNumber()).thenReturn(ORGANIZATION_NUMBER);
		when(messagingMapperMock.toDigitalMailRequestSimplifiedService(any(), eq(MUNICIPALITY_ID))).thenReturn(digitalMailRequest);
		when(messagingClientMock.sendDigitalMail(eq(MUNICIPALITY_ID), any(), any())).thenReturn(messageBatchResult);

		// Act
		final var messageId = messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand);

		// Assert
		assertThat(messageId).isNull();
		verify(messagingClientMock).sendDigitalMail(MUNICIPALITY_ID, ORGANIZATION_NUMBER, digitalMailRequest);
		verify(messagingClientMock, never()).sendWebMessage(eq(MUNICIPALITY_ID), any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void sendDecisionMessage() {

		// Arrange
		final var errand = createErrand(false);
		final var renderResponse = new RenderResponse();
		final var digitalMailRequest = new DigitalMailRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());
		final var messageBatchResult = new MessageBatchResult().addMessagesItem(messageResult);

		when(textPropertiesMock.getCommons()).thenReturn(Map.of(MUNICIPALITY_ID, commonTextPropertiesMock));
		when(commonTextPropertiesMock.getOrganizationNumber()).thenReturn(ORGANIZATION_NUMBER);
		when(messagingMapperMock.toDigitalMailRequest(any(), any(), eq(MUNICIPALITY_ID), eq(true))).thenReturn(digitalMailRequest);
		when(messagingClientMock.sendDigitalMail(eq(MUNICIPALITY_ID), any(), any())).thenReturn(messageBatchResult);

		// Act
		final var uuid = messagingService.sendDecisionMessage(MUNICIPALITY_ID, errand, renderResponse, true);

		// Assert
		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendDigitalMail(MUNICIPALITY_ID, ORGANIZATION_NUMBER, digitalMailRequest);
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void sendDecisionWebMessage() {

		// Arrange
		final var errand = createErrand(false);
		final var renderResponse = new RenderResponse();
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());
		final var decision = new Decision().decisionType(FINAL).decisionOutcome(Decision.DecisionOutcomeEnum.APPROVAL);

		when(messagingMapperMock.toWebMessageRequestDecision(any(), any(), any(), eq(MUNICIPALITY_ID), any())).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var uuid = messagingService.sendDecisionWebMessage(MUNICIPALITY_ID, errand, renderResponse, decision);

		// Assert
		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromSendDigitalMailResource() {

		// Arrange
		final var errand = createErrand(false);
		final var renderResponse = new RenderResponse();
		final var digitalMailRequest = new DigitalMailRequest();
		final var messageResult = new MessageResult();
		final var messageBatchResult = new MessageBatchResult().addMessagesItem(messageResult);

		when(textPropertiesMock.getCommons()).thenReturn(Map.of(MUNICIPALITY_ID, commonTextPropertiesMock));
		when(commonTextPropertiesMock.getOrganizationNumber()).thenReturn(ORGANIZATION_NUMBER);
		when(messagingMapperMock.toDigitalMailRequest(any(), any(), eq(MUNICIPALITY_ID), eq(true))).thenReturn(digitalMailRequest);
		when(messagingClientMock.sendDigitalMail(eq(MUNICIPALITY_ID), any(), any())).thenReturn(messageBatchResult);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendDecisionMessage(MUNICIPALITY_ID, errand, renderResponse, true));

		// Assert
		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingClientMock).sendDigitalMail(MUNICIPALITY_ID, ORGANIZATION_NUMBER, digitalMailRequest);
		verifyNoInteractions(templatingClientMock);
	}

	private static Errand createErrand(boolean withExternalCaseId) {
		return new Errand()
			.externalCaseId(withExternalCaseId ? "1234" : null)
			.stakeholders(List.of(createStakeholder(ROLE_APPLICANT), createStakeholder(ROLE_ADMINISTRATOR)));
	}

	public static Stakeholder createStakeholder(String role) {
		return new Stakeholder()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}
}
