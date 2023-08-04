package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.LetterAttachment.ContentTypeEnum.APPLICATION_PDF;
import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import generated.se.sundsvall.messaging.LetterAttachment;
import generated.se.sundsvall.messaging.LetterAttachment.DeliveryModeEnum;
import generated.se.sundsvall.messaging.LetterParty;
import generated.se.sundsvall.messaging.LetterRequest.ContentTypeEnum;
import generated.se.sundsvall.messaging.LetterSenderSupportInfo;
import generated.se.sundsvall.messaging.WebMessageAttachment;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.util.CommonTextProperties;
import se.sundsvall.parkingpermit.util.DenialTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("junit")
class MessagingMapperTest {

	private static final UUID PARTY_ID = UUID.randomUUID();
	private static final String OUTPUT = "output";
	private static final RenderResponse RENDER_RESPONSE = new RenderResponse().output(OUTPUT);

	private static final String DEPARTMENT = "department";
	private static final String FILENAME = "filename";
	private static final String MESSAGE = "message";
	private static final String SUBJECT = "subject";
	private static final String BODY = "body";
	private static final String CONTACTINFO_EMAIL = "contactinfoEmail";
	private static final String CONTACTINFO_PHONENUMBER = "contactinfoPhonenumber";
	private static final String CONTACTINFO_TEXT = "contactinfoText";
	private static final String CONTACTINFO_URL = "contactinfoUrl";

	@Mock
	private CommonTextProperties commonTextPropertiesMock;

	@Mock
	private DenialTextProperties denialTextPropertiesMock;

	@Mock
	private TextProvider textProviderMock;

	@InjectMocks
	private MessagingMapper messagingMapper;

	@Test
	void toWebMessageRequest() {
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.filename()).thenReturn(FILENAME);
		when(denialTextPropertiesMock.message()).thenReturn(MESSAGE);

		final var request = messagingMapper.toWebMessageRequest(RENDER_RESPONSE, PARTY_ID.toString());

		assertThat(request.getParty()).isNotNull().extracting(WebMessageParty::getPartyId).isEqualTo(PARTY_ID);
		assertThat(request.getMessage()).isEqualTo(MESSAGE);
		assertThat(request.getAttachments()).hasSize(1)
			.extracting(
				WebMessageAttachment::getBase64Data,
				WebMessageAttachment::getFileName,
				WebMessageAttachment::getMimeType)
			.containsExactly(tuple(
				OUTPUT,
				FILENAME,
				APPLICATION_PDF.value()));

		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).message();
		verifyNoMoreInteractions(commonTextPropertiesMock);
	}

	@Test
	void toLetterRequest() {
		when(textProviderMock.getCommonTexts()).thenReturn(commonTextPropertiesMock);
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(commonTextPropertiesMock.contactInfoEmail()).thenReturn(CONTACTINFO_EMAIL);
		when(commonTextPropertiesMock.contactInfoPhonenumber()).thenReturn(CONTACTINFO_PHONENUMBER);
		when(commonTextPropertiesMock.contactInfoText()).thenReturn(CONTACTINFO_TEXT);
		when(commonTextPropertiesMock.contactInfoUrl()).thenReturn(CONTACTINFO_URL);
		when(commonTextPropertiesMock.department()).thenReturn(DEPARTMENT);
		when(denialTextPropertiesMock.subject()).thenReturn(SUBJECT);
		when(denialTextPropertiesMock.htmlBody()).thenReturn(BODY);
		when(denialTextPropertiesMock.filename()).thenReturn(FILENAME);

		final var request = messagingMapper.toLetterRequest(RENDER_RESPONSE, PARTY_ID.toString());

		assertThat(request.getSubject()).isEqualTo(SUBJECT);
		assertThat(Base64.getDecoder().decode(request.getBody())).isEqualTo(BODY.getBytes(defaultCharset()));
		assertThat(request.getContentType()).isEqualTo(ContentTypeEnum.HTML);
		assertThat(request.getSender()).isNotNull();
		assertThat(request.getSender().getSupportInfo()).isNotNull()
			.extracting(
				LetterSenderSupportInfo::getEmailAddress,
				LetterSenderSupportInfo::getPhoneNumber,
				LetterSenderSupportInfo::getText,
				LetterSenderSupportInfo::getUrl)
			.containsExactlyInAnyOrder(
				CONTACTINFO_EMAIL,
				CONTACTINFO_PHONENUMBER,
				CONTACTINFO_TEXT,
				CONTACTINFO_URL);
		assertThat(request.getParty()).isNotNull().extracting(LetterParty::getPartyIds).asList().containsExactly(PARTY_ID);
		assertThat(request.getDepartment()).isEqualTo(DEPARTMENT);
		assertThat(request.getAttachments()).hasSize(1)
			.extracting(
				LetterAttachment::getContent,
				LetterAttachment::getContentType,
				LetterAttachment::getDeliveryMode,
				LetterAttachment::getFilename)
			.containsExactly(tuple(
				OUTPUT,
				APPLICATION_PDF,
				DeliveryModeEnum.ANY,
				FILENAME));

		verify(commonTextPropertiesMock).contactInfoEmail();
		verify(commonTextPropertiesMock).contactInfoPhonenumber();
		verify(commonTextPropertiesMock).contactInfoText();
		verify(commonTextPropertiesMock).contactInfoUrl();
		verify(commonTextPropertiesMock).department();
		verify(denialTextPropertiesMock).htmlBody();
		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).subject();
	}
}
