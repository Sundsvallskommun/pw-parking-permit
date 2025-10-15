package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.messaging.LetterAttachment.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.WebMessageRequest.OepInstanceEnum.EXTERNAL;
import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.MESSAGING_KEY_FLOW_INSTANCE_ID;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailParty;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.DigitalMailSenderSupportInfo;
import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.LetterAttachment;
import generated.se.sundsvall.messaging.LetterAttachment.DeliveryModeEnum;
import generated.se.sundsvall.messaging.LetterParty;
import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.LetterSenderSupportInfo;
import generated.se.sundsvall.messaging.WebMessageAttachment;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.util.ApprovalTextProperties;
import se.sundsvall.parkingpermit.util.CommonTextProperties;
import se.sundsvall.parkingpermit.util.DenialTextProperties;
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
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
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private CommonTextProperties commonTextPropertiesMock;

	@Mock
	private DenialTextProperties denialTextPropertiesMock;

	@Mock
	private ApprovalTextProperties approvalTextPropertiesMock;

	@Mock
	private SimplifiedServiceTextProperties simplifiedServiceTextPropertiesMock;

	@Mock
	private TextProvider textProviderMock;

	@InjectMocks
	private MessagingMapper messagingMapper;

	@Test
	void toWebMessageRequestDenial() {
		final var externalCaseId = "externalCaseId";

		when(textProviderMock.getDenialTexts(MUNICIPALITY_ID)).thenReturn(denialTextPropertiesMock);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(commonTextPropertiesMock.getFilename()).thenReturn(FILENAME);
		when(denialTextPropertiesMock.getMessage()).thenReturn(MESSAGE);

		final var request = messagingMapper.toWebMessageRequestDenial(RENDER_RESPONSE, PARTY_ID.toString(), externalCaseId,
			MUNICIPALITY_ID);

		assertThat(request.getParty()).isNotNull().extracting(WebMessageParty::getPartyId, WebMessageParty::getExternalReferences).containsExactly(
			PARTY_ID,
			List.of(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
		assertThat(request.getOepInstance()).isEqualTo(EXTERNAL);
		assertThat(request.getMessage()).isEqualTo(MESSAGE);
		assertThat(request.getAttachments()).hasSize(1)
			.extracting(
				WebMessageAttachment::getBase64Data,
				WebMessageAttachment::getFileName,
				WebMessageAttachment::getMimeType)
			.containsExactly(tuple(
				OUTPUT,
				FILENAME,
				APPLICATION_PDF.getValue()));

		verify(textProviderMock).getDenialTexts(MUNICIPALITY_ID);
		verify(textProviderMock).getCommonTexts(MUNICIPALITY_ID);
		verify(commonTextPropertiesMock).getFilename();
		verify(denialTextPropertiesMock).getMessage();
		verifyNoMoreInteractions(commonTextPropertiesMock);
	}

	@Test
	void toWebMessageRequestDecision() {
		final var externalCaseId = "externalCaseId";
		final var decisionDescription = "decisionDescription";
		final var decision = new Decision().decisionType(FINAL).decisionOutcome(APPROVAL).description(decisionDescription);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(commonTextPropertiesMock.getFilename()).thenReturn(FILENAME);

		final var request = messagingMapper.toWebMessageRequestDecision(RENDER_RESPONSE, PARTY_ID.toString(), externalCaseId,
			MUNICIPALITY_ID, decision);

		assertThat(request.getParty()).isNotNull().extracting(WebMessageParty::getPartyId, WebMessageParty::getExternalReferences).containsExactly(
			PARTY_ID,
			List.of(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
		assertThat(request.getOepInstance()).isEqualTo(EXTERNAL);
		assertThat(request.getMessage()).isEqualTo(decisionDescription);
		assertThat(request.getAttachments()).hasSize(1)
			.extracting(
				WebMessageAttachment::getBase64Data,
				WebMessageAttachment::getFileName,
				WebMessageAttachment::getMimeType)
			.containsExactly(tuple(
				OUTPUT,
				FILENAME,
				APPLICATION_PDF.getValue()));

		verify(commonTextPropertiesMock).getFilename();
		verifyNoInteractions(approvalTextPropertiesMock, simplifiedServiceTextPropertiesMock);
	}

	@Test
	void toWebMessageRequestSimplifiedService() {
		final var externalCaseId = "externalCaseId";

		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(simplifiedServiceTextPropertiesMock.getPlainBody()).thenReturn(MESSAGE);

		final var request = messagingMapper.toWebMessageRequestSimplifiedService(PARTY_ID.toString(), externalCaseId, MUNICIPALITY_ID);

		assertThat(request.getParty()).isNotNull().extracting(WebMessageParty::getPartyId, WebMessageParty::getExternalReferences).containsExactly(
			PARTY_ID,
			List.of(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
		assertThat(request.getOepInstance()).isEqualTo(EXTERNAL);
		assertThat(request.getMessage()).isEqualTo(MESSAGE);
		assertThat(request.getAttachments()).isNull();

		verify(textProviderMock).getSimplifiedServiceTexts(MUNICIPALITY_ID);
		verify(simplifiedServiceTextPropertiesMock).getPlainBody();
		verifyNoMoreInteractions(commonTextPropertiesMock);
	}

	@Test
	void toLetterRequestDenial() {
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(textProviderMock.getDenialTexts(MUNICIPALITY_ID)).thenReturn(denialTextPropertiesMock);
		when(commonTextPropertiesMock.getContactInfoEmail()).thenReturn(CONTACTINFO_EMAIL);
		when(commonTextPropertiesMock.getContactInfoPhonenumber()).thenReturn(CONTACTINFO_PHONENUMBER);
		when(commonTextPropertiesMock.getContactInfoText()).thenReturn(CONTACTINFO_TEXT);
		when(commonTextPropertiesMock.getContactInfoUrl()).thenReturn(CONTACTINFO_URL);
		when(commonTextPropertiesMock.getDepartment()).thenReturn(DEPARTMENT);
		when(denialTextPropertiesMock.getSubject()).thenReturn(SUBJECT);
		when(denialTextPropertiesMock.getHtmlBody()).thenReturn(BODY);
		when(commonTextPropertiesMock.getFilename()).thenReturn(FILENAME);

		final var request = messagingMapper.toLetterRequestDenial(RENDER_RESPONSE, PARTY_ID.toString(), MUNICIPALITY_ID);

		assertThat(request.getSubject()).isEqualTo(SUBJECT);
		assertThat(Base64.getDecoder().decode(request.getBody())).isEqualTo(BODY.getBytes(defaultCharset()));
		assertThat(request.getContentType()).isEqualTo(LetterRequest.ContentTypeEnum.TEXT_HTML);
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
		assertThat(request.getParty()).isNotNull().extracting(LetterParty::getPartyIds).asInstanceOf(LIST).containsExactly(PARTY_ID);
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

		verify(textProviderMock, times(6)).getCommonTexts(MUNICIPALITY_ID);
		verify(textProviderMock, times(2)).getDenialTexts(MUNICIPALITY_ID);
		verify(commonTextPropertiesMock).getContactInfoEmail();
		verify(commonTextPropertiesMock).getContactInfoPhonenumber();
		verify(commonTextPropertiesMock).getContactInfoText();
		verify(commonTextPropertiesMock).getContactInfoUrl();
		verify(commonTextPropertiesMock).getDepartment();
		verify(denialTextPropertiesMock).getHtmlBody();
		verify(commonTextPropertiesMock).getFilename();
		verify(denialTextPropertiesMock).getSubject();
	}

	@Test
	void toLetterRequestSimplifiedService() {
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(commonTextPropertiesMock.getContactInfoEmail()).thenReturn(CONTACTINFO_EMAIL);
		when(commonTextPropertiesMock.getContactInfoPhonenumber()).thenReturn(CONTACTINFO_PHONENUMBER);
		when(commonTextPropertiesMock.getContactInfoText()).thenReturn(CONTACTINFO_TEXT);
		when(commonTextPropertiesMock.getContactInfoUrl()).thenReturn(CONTACTINFO_URL);
		when(commonTextPropertiesMock.getDepartment()).thenReturn(DEPARTMENT);
		when(simplifiedServiceTextPropertiesMock.getSubject()).thenReturn(SUBJECT);
		when(simplifiedServiceTextPropertiesMock.getHtmlBody()).thenReturn(BODY);

		final var request = messagingMapper.toLetterRequestSimplifiedService(PARTY_ID.toString(), MUNICIPALITY_ID);

		assertThat(request.getSubject()).isEqualTo(SUBJECT);
		assertThat(Base64.getDecoder().decode(request.getBody())).isEqualTo(BODY.getBytes(defaultCharset()));
		assertThat(request.getContentType()).isEqualTo(LetterRequest.ContentTypeEnum.TEXT_HTML);
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
		assertThat(request.getParty()).isNotNull().extracting(LetterParty::getPartyIds).asInstanceOf(LIST).containsExactly(PARTY_ID);
		assertThat(request.getDepartment()).isEqualTo(DEPARTMENT);
		assertThat(request.getAttachments()).isNull();

		verify(textProviderMock, times(5)).getCommonTexts(MUNICIPALITY_ID);
		verify(textProviderMock, times(2)).getSimplifiedServiceTexts(MUNICIPALITY_ID);
		verify(commonTextPropertiesMock).getContactInfoEmail();
		verify(commonTextPropertiesMock).getContactInfoPhonenumber();
		verify(commonTextPropertiesMock).getContactInfoText();
		verify(commonTextPropertiesMock).getContactInfoUrl();
		verify(commonTextPropertiesMock).getDepartment();
		verify(simplifiedServiceTextPropertiesMock).getHtmlBody();
		verify(simplifiedServiceTextPropertiesMock).getSubject();
	}

	@Test
	void toDigitalMailRequestApproval() {
		// Arrange
		when(textProviderMock.getApprovalTexts(MUNICIPALITY_ID)).thenReturn(approvalTextPropertiesMock);
		when(approvalTextPropertiesMock.getHtmlBody()).thenReturn(BODY);
		when(approvalTextPropertiesMock.getSubject()).thenReturn(SUBJECT);

		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(commonTextPropertiesMock.getDepartment()).thenReturn(DEPARTMENT);
		when(commonTextPropertiesMock.getContactInfoEmail()).thenReturn(CONTACTINFO_EMAIL);
		when(commonTextPropertiesMock.getContactInfoPhonenumber()).thenReturn(CONTACTINFO_PHONENUMBER);
		when(commonTextPropertiesMock.getContactInfoText()).thenReturn(CONTACTINFO_TEXT);
		when(commonTextPropertiesMock.getContactInfoUrl()).thenReturn(CONTACTINFO_URL);
		when(commonTextPropertiesMock.getFilename()).thenReturn(FILENAME);

		// Act
		final var request = messagingMapper.toDigitalMailRequest(RENDER_RESPONSE, PARTY_ID.toString(), MUNICIPALITY_ID, true);

		// Assert and verify
		assertThat(request.getSubject()).isEqualTo(SUBJECT);
		assertThat(Base64.getDecoder().decode(request.getBody())).isEqualTo(BODY.getBytes(defaultCharset()));
		assertThat(request.getContentType()).isEqualTo(DigitalMailRequest.ContentTypeEnum.TEXT_HTML);
		assertThat(request.getSender()).isNotNull();
		assertThat(request.getSender().getSupportInfo()).isNotNull()
			.extracting(
				DigitalMailSenderSupportInfo::getEmailAddress,
				DigitalMailSenderSupportInfo::getPhoneNumber,
				DigitalMailSenderSupportInfo::getText,
				DigitalMailSenderSupportInfo::getUrl)
			.containsExactlyInAnyOrder(
				CONTACTINFO_EMAIL,
				CONTACTINFO_PHONENUMBER,
				CONTACTINFO_TEXT,
				CONTACTINFO_URL);
		assertThat(request.getAttachments()).hasSize(1).extracting(DigitalMailAttachment::getContent,
			DigitalMailAttachment::getContentType,
			DigitalMailAttachment::getFilename)
			.containsExactly(tuple(
				OUTPUT,
				DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF,
				FILENAME));
		assertThat(request.getParty()).isNotNull().extracting(DigitalMailParty::getPartyIds).asInstanceOf(LIST).containsExactly(PARTY_ID);
		assertThat(request.getDepartment()).isEqualTo(DEPARTMENT);

		verifyNoMoreInteractions(textProviderMock, commonTextPropertiesMock);
	}
}
