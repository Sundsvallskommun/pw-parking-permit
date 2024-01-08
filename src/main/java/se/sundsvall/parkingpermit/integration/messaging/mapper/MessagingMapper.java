package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.LetterAttachment.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.LetterAttachment.DeliveryModeEnum.ANY;
import static generated.se.sundsvall.messaging.LetterRequest.ContentTypeEnum.HTML;
import static java.nio.charset.Charset.defaultCharset;

import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import generated.se.sundsvall.messaging.LetterAttachment;
import generated.se.sundsvall.messaging.LetterParty;
import generated.se.sundsvall.messaging.LetterRequest;
import generated.se.sundsvall.messaging.LetterSender;
import generated.se.sundsvall.messaging.LetterSenderSupportInfo;
import generated.se.sundsvall.messaging.WebMessageAttachment;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.messaging.WebMessageRequest;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.util.TextProvider;

@Service
public class MessagingMapper {

	@Autowired
	private TextProvider textProvider;

	public WebMessageRequest toWebMessageRequest(RenderResponse renderResponse, String partyId) {
		return new WebMessageRequest()
			.addAttachmentsItem(toWebMessageAttachment(renderResponse))
			.message(textProvider.getDenialTexts().message())
			.party(new WebMessageParty().partyId(UUID.fromString(partyId)));
	}

	private WebMessageAttachment toWebMessageAttachment(RenderResponse renderResponse) {
		return new WebMessageAttachment()
			.base64Data(renderResponse.getOutput())
			.fileName(textProvider.getDenialTexts().filename())
			.mimeType(APPLICATION_PDF.getValue());
	}

	public LetterRequest toLetterRequest(RenderResponse renderResponse, String partyId) {
		return new LetterRequest()
			.addAttachmentsItem(toLetterAttachment(renderResponse))
			.body(Base64.getEncoder().encodeToString(textProvider.getDenialTexts().htmlBody().getBytes(defaultCharset())))
			.contentType(HTML)
			.department(textProvider.getCommonTexts().department())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender())
			.subject(textProvider.getDenialTexts().subject());
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
