package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.LetterAttachment.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.LetterAttachment.DeliveryModeEnum.ANY;
import static generated.se.sundsvall.messaging.LetterRequest.ContentTypeEnum.HTML;
import static generated.se.sundsvall.messaging.WebMessageRequest.OepInstanceEnum.EXTERNAL;
import static java.nio.charset.Charset.defaultCharset;
import static se.sundsvall.parkingpermit.Constants.MESSAGING_KEY_FLOW_INSTANCE_ID;

import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.LetterAttachment;
import generated.se.sundsvall.messaging.LetterParty;
import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.LetterSender;
import generated.se.sundsvall.messaging.LetterSenderSupportInfo;
import generated.se.sundsvall.messaging.WebMessageAttachment;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.messaging.WebMessageRequest;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.util.TextProvider;

@Service
public class MessagingMapper {

	private final TextProvider textProvider;

	MessagingMapper(TextProvider textProvider) {
		this.textProvider = textProvider;
	}

	public WebMessageRequest toWebMessageRequestDenial(RenderResponse renderResponse, String partyId, String externalCaseId) {
		return new WebMessageRequest()
			.addAttachmentsItem(toWebMessageAttachment(renderResponse))
			.message(textProvider.getDenialTexts().message())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	public WebMessageRequest toWebMessageRequestSimplifiedService(String partyId, String externalCaseId) {
		return new WebMessageRequest()
			.message(textProvider.getSimplifiedServiceTexts().plainBody())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	private WebMessageAttachment toWebMessageAttachment(RenderResponse renderResponse) {
		return new WebMessageAttachment()
			.base64Data(renderResponse.getOutput())
			.fileName(textProvider.getDenialTexts().filename())
			.mimeType(APPLICATION_PDF.getValue());
	}

	public LetterRequest toLetterRequestDenial(RenderResponse renderResponse, String partyId) {
		return new LetterRequest()
			.addAttachmentsItem(toLetterAttachment(renderResponse))
			.body(Base64.getEncoder().encodeToString(textProvider.getDenialTexts().htmlBody().getBytes(defaultCharset())))
			.contentType(HTML)
			.department(textProvider.getCommonTexts().department())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender())
			.subject(textProvider.getDenialTexts().subject());
	}

	public LetterRequest toLetterRequestSimplifiedService(String partyId) {
		return new LetterRequest()
			.body(Base64.getEncoder().encodeToString(textProvider.getSimplifiedServiceTexts().htmlBody().getBytes(defaultCharset())))
			.contentType(HTML)
			.department(textProvider.getCommonTexts().department())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender())
			.subject(textProvider.getSimplifiedServiceTexts().subject());
	}

	private LetterSender toLetterSender() {
		return new LetterSender()
			.supportInfo(toLetterSenderSupportInfo());
	}

	private LetterSenderSupportInfo toLetterSenderSupportInfo() {
		return new LetterSenderSupportInfo()
			.emailAddress(textProvider.getCommonTexts().contactInfoEmail())
			.phoneNumber(textProvider.getCommonTexts().contactInfoPhonenumber())
			.text(textProvider.getCommonTexts().contactInfoText())
			.url(textProvider.getCommonTexts().contactInfoUrl());
	}

	private LetterAttachment toLetterAttachment(RenderResponse renderResponse) {
		return new LetterAttachment()
			.content(renderResponse.getOutput())
			.contentType(APPLICATION_PDF)
			.deliveryMode(ANY)
			.filename(textProvider.getDenialTexts().filename());
	}
}
