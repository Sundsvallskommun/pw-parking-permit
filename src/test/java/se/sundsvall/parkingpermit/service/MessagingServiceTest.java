package se.sundsvall.parkingpermit.service;

import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.APPLICANT;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.WebMessageRequest;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.integration.messaging.MessagingClient;
import se.sundsvall.parkingpermit.integration.messaging.mapper.MessagingMapper;
import se.sundsvall.parkingpermit.integration.templating.TemplatingClient;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {
	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private TemplatingClient templatingClientMock;
	
	@Mock
	private RenderResponse renderResponseMock;
	
	@Mock
	private MessagingMapper messagingMapperMock;

	@InjectMocks
	private MessagingService messagingService;
	
	@Captor
	ArgumentCaptor<LetterRequest> letterRequestCaptor;
	
	@Captor
	ArgumentCaptor<WebMessageRequest> webMessageRequestCaptor;

	@Test
	void renderPdf() throws Exception {
		when(templatingClientMock.renderPdf(any())).thenReturn(renderResponseMock);

		messagingService.renderPdf(createErrand(true));

		verify(templatingClientMock).renderPdf(any());
		verifyNoInteractions(messagingClientMock, messagingMapperMock);
	}
	
	@Test
	void getProperties() {
		messagingService.getProperties();

		verify(messagingMapperMock).getProperties();
		verifyNoInteractions(messagingClientMock, templatingClientMock);
	}

	@Test
	void sendMessageToNonCitizenWithExternalCaseIdPresentInErrand() throws Exception {
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingMapperMock.toWebMessageRequest(any(), any())).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(any())).thenReturn(messageResult);
		
		final var uuid = messagingService.sendMessageToNonCitizen(createErrand(true), new RenderResponse());

		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendWebMessage(webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void sendMessageToNonCitizenWithExternalCaseIdAbsent() throws Exception {
		final var letterRequest = new LetterRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());
		final var messageBatchResult = new MessageBatchResult().addMessagesItem(messageResult);

		when(messagingMapperMock.toLetterRequest(any(), any())).thenReturn(letterRequest);
		when(messagingClientMock.sendLetter(any())).thenReturn(messageBatchResult);

		final var uuid = messagingService.sendMessageToNonCitizen(createErrand(false), new RenderResponse());

		assertThat(uuid).isEqualTo(messageResult.getMessageId());
		verify(messagingClientMock).sendLetter(letterRequest);
		verify(messagingClientMock, never()).sendWebMessage(any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingWebmessageResource() throws Exception {
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult();

		when(messagingMapperMock.toWebMessageRequest(any(), any())).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(any())).thenReturn(messageResult);

		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendMessageToNonCitizen(createErrand(true), new RenderResponse()));

		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingClientMock).sendWebMessage(webMessageRequest);
		verify(messagingClientMock, never()).sendLetter(any());
		verifyNoInteractions(templatingClientMock);
	}

	@Test
	void noMessageIdReturnedFromMessagingLetterResource() throws Exception {
		final var letterRequest = new LetterRequest();
		final var messageBatchResult = new MessageBatchResult();

		when(messagingMapperMock.toLetterRequest(any(), any())).thenReturn(letterRequest);
		when(messagingClientMock.sendLetter(any())).thenReturn(messageBatchResult);

		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendMessageToNonCitizen(createErrand(false), new RenderResponse()));

		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingClientMock).sendLetter(letterRequest);
		verify(messagingClientMock, never()).sendWebMessage(any());
		verifyNoInteractions(templatingClientMock);
	}

	private static ErrandDTO createErrand(boolean withExternalCaseId) {
		return new ErrandDTO()
			.externalCaseId(withExternalCaseId ? "1234" : null)
			.stakeholders(List.of(createStakeholder(APPLICANT), createStakeholder(ADMINISTRATOR)));
	}

	public static StakeholderDTO createStakeholder(RolesEnum role) {
		return new StakeholderDTO()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}
}
