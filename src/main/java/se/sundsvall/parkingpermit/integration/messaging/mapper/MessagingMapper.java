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

	public WebMessageRequest toWebMessageRequestDenial(RenderResponse renderResponse, String partyId, String externalCaseId, String municipalityId) {
		return new WebMessageRequest()
			.addAttachmentsItem(toWebMessageAttachment(renderResponse, municipalityId))
			.message(textProvider.getDenialTexts(municipalityId).getMessage())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	public WebMessageRequest toWebMessageRequestSimplifiedService(String partyId, String externalCaseId, String municipalityId) {
		return new WebMessageRequest()
			.message(textProvider.getSimplifiedServiceTexts(municipalityId).getPlainBody())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	private WebMessageAttachment toWebMessageAttachment(RenderResponse renderResponse, String municipalityId) {
		return new WebMessageAttachment()
			.base64Data(renderResponse.getOutput())
			.fileName(textProvider.getDenialTexts(municipalityId).getFilename())
			.mimeType(APPLICATION_PDF.getValue());
	}

	public LetterRequest toLetterRequestDenial(RenderResponse renderResponse, String partyId, String municipalityId) {
		return new LetterRequest()
			.addAttachmentsItem(toLetterAttachment(renderResponse, municipalityId))
			.body(Base64.getEncoder().encodeToString(textProvider.getDenialTexts(municipalityId).getHtmlBody().getBytes(defaultCharset())))
			.contentType(HTML)
			.department(textProvider.getCommonTexts(municipalityId).getDepartment())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender(municipalityId))
			.subject(textProvider.getDenialTexts(municipalityId).getSubject());
	}

	public LetterRequest toLetterRequestSimplifiedService(String partyId, String municipalityId) {
		return new LetterRequest()
			.body(Base64.getEncoder().encodeToString(textProvider.getSimplifiedServiceTexts(municipalityId).getHtmlBody().getBytes(defaultCharset())))
			.contentType(HTML)
			.department(textProvider.getCommonTexts(municipalityId).getDepartment())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender(municipalityId))
			.subject(textProvider.getSimplifiedServiceTexts(municipalityId).getSubject());
	}

	private LetterSender toLetterSender(String municipalityId) {
		return new LetterSender()
			.supportInfo(toLetterSenderSupportInfo(municipalityId));
	}

	private LetterSenderSupportInfo toLetterSenderSupportInfo(String municipalityId) {
		return new LetterSenderSupportInfo()
			.emailAddress(textProvider.getCommonTexts(municipalityId).getContactInfoEmail())
			.phoneNumber(textProvider.getCommonTexts(municipalityId).getContactInfoPhonenumber())
			.text(textProvider.getCommonTexts(municipalityId).getContactInfoText())
			.url(textProvider.getCommonTexts(municipalityId).getContactInfoUrl());
	}

	private LetterAttachment toLetterAttachment(RenderResponse renderResponse, String municipalityId) {
		return new LetterAttachment()
			.content(renderResponse.getOutput())
			.contentType(APPLICATION_PDF)
			.deliveryMode(ANY)
			.filename(textProvider.getDenialTexts(municipalityId).getFilename());
	}
}
