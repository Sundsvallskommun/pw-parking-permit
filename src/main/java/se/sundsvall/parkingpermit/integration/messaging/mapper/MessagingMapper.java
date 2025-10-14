package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.LetterAttachment.ContentTypeEnum.APPLICATION_PDF;
import static generated.se.sundsvall.messaging.LetterAttachment.DeliveryModeEnum.ANY;
import static generated.se.sundsvall.messaging.LetterRequest.ContentTypeEnum.TEXT_HTML;
import static generated.se.sundsvall.messaging.WebMessageRequest.OepInstanceEnum.EXTERNAL;
import static java.nio.charset.Charset.defaultCharset;
import static se.sundsvall.parkingpermit.Constants.MESSAGING_KEY_FLOW_INSTANCE_ID;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailParty;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.DigitalMailSender;
import generated.se.sundsvall.messaging.DigitalMailSenderSupportInfo;
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

	public WebMessageRequest toWebMessageRequestDecision(RenderResponse renderResponse, String partyId, String externalCaseId, String municipalityId, Decision decision) {
		return new WebMessageRequest()
			.addAttachmentsItem(toWebMessageAttachment(renderResponse, municipalityId))
			.message(decision.getDescription())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	public WebMessageRequest toWebMessageRequestSimplifiedService(String partyId, String externalCaseId, String municipalityId) {
		return new WebMessageRequest()
			.message(textProvider.getSimplifiedServiceTexts(municipalityId).getPlainBody())
			.attachments(null)
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}

	private WebMessageAttachment toWebMessageAttachment(RenderResponse renderResponse, String municipalityId) {
		return new WebMessageAttachment()
			.base64Data(renderResponse.getOutput())
			.fileName(textProvider.getCommonTexts(municipalityId).getFilename())
			.mimeType(APPLICATION_PDF.getValue());
	}

	public LetterRequest toLetterRequestDenial(RenderResponse renderResponse, String partyId, String municipalityId) {
		return new LetterRequest()
			.addAttachmentsItem(toLetterAttachment(renderResponse, municipalityId))
			.body(Base64.getEncoder().encodeToString(textProvider.getDenialTexts(municipalityId).getHtmlBody().getBytes(defaultCharset())))
			.contentType(TEXT_HTML)
			.department(textProvider.getCommonTexts(municipalityId).getDepartment())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender(municipalityId))
			.subject(textProvider.getDenialTexts(municipalityId).getSubject());
	}

	public LetterRequest toLetterRequestSimplifiedService(String partyId, String municipalityId) {
		return new LetterRequest()
			.body(Base64.getEncoder().encodeToString(textProvider.getSimplifiedServiceTexts(municipalityId).getHtmlBody().getBytes(defaultCharset())))
			.contentType(TEXT_HTML)
			.department(textProvider.getCommonTexts(municipalityId).getDepartment())
			.party(new LetterParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toLetterSender(municipalityId))
			.subject(textProvider.getSimplifiedServiceTexts(municipalityId).getSubject())
			.attachments(null);
	}

	public DigitalMailRequest toDigitalMailRequest(RenderResponse renderResponse, String partyId, String municipalityId, boolean isApproval) {
		final var htmlBody = isApproval
			? textProvider.getApprovalTexts(municipalityId).getHtmlBody()
			: textProvider.getDenialTexts(municipalityId).getHtmlBody();
		final var subject = isApproval
			? textProvider.getApprovalTexts(municipalityId).getSubject()
			: textProvider.getDenialTexts(municipalityId).getSubject();
		final var filename = textProvider.getCommonTexts(municipalityId).getFilename();

		return new DigitalMailRequest()
			.addAttachmentsItem(toDigitalMailAttachment(renderResponse, filename))
			.body(Base64.getEncoder().encodeToString(htmlBody.getBytes(defaultCharset())))
			.contentType(DigitalMailRequest.ContentTypeEnum.TEXT_HTML)
			.department(textProvider.getCommonTexts(municipalityId).getDepartment())
			.party(new DigitalMailParty().addPartyIdsItem(UUID.fromString(partyId)))
			.sender(toDigitalMailSender(municipalityId))
			.subject(subject);
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

	private DigitalMailSenderSupportInfo toDigitalMailSenderSupportInfo(String municipalityId) {
		return new DigitalMailSenderSupportInfo()
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
			.filename(textProvider.getCommonTexts(municipalityId).getFilename());
	}

	private DigitalMailAttachment toDigitalMailAttachment(RenderResponse renderResponse, String filename) {
		return new DigitalMailAttachment()
			.content(renderResponse.getOutput())
			.contentType(DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF)
			.filename(filename);
	}

	private DigitalMailSender toDigitalMailSender(String municipalityId) {
		return new DigitalMailSender()
			.supportInfo(toDigitalMailSenderSupportInfo(municipalityId));
	}
}
