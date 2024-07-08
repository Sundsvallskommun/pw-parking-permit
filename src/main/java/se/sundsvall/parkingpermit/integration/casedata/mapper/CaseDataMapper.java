package se.sundsvall.parkingpermit.integration.casedata.mapper;

import generated.se.sundsvall.casedata.AttachmentDTO;
import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.LawDTO;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.MessageRequest.DirectionEnum;
import generated.se.sundsvall.casedata.PatchErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum;
import generated.se.sundsvall.casedata.StatusDTO;
import generated.se.sundsvall.templating.RenderResponse;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;

public class CaseDataMapper {

	private CaseDataMapper() {}

	public static PatchErrandDTO toPatchErrand(String externalCaseId, String phase, String phaseStatus, String phaseAction, String displayPhase) {
		return new PatchErrandDTO()
			.externalCaseId(externalCaseId)
			.phase(phase)
			.putExtraParametersItem(CASEDATA_KEY_PHASE_STATUS, phaseStatus)
			.putExtraParametersItem(CASEDATA_KEY_DISPLAY_PHASE, displayPhase)
			.putExtraParametersItem(CASEDATA_KEY_PHASE_ACTION, phaseAction);
	}

	public static StakeholderDTO toStakeholder(String role, TypeEnum type, String firstName, String lastName) {
		final var bean = new StakeholderDTO()
			.firstName(firstName)
			.lastName(lastName)
			.type(type);

		ofNullable(role).ifPresent(bean::addRolesItem);

		return bean;
	}

	public static DecisionDTO toDecision(DecisionTypeEnum decisionType, DecisionOutcomeEnum decisionOutcome, String description) {
		return new DecisionDTO()
			.created(getNow())
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcome)
			.description(description);
	}

	public static LawDTO toLaw(String heading, String sfs, String chapter, String article) {
		return new LawDTO()
			.heading(heading)
			.sfs(sfs)
			.chapter(chapter)
			.article(article);
	}

	public static AttachmentDTO toAttachment(String category, String name, String extension, String mimeType, RenderResponse renderedContent) {
		final var bean = new AttachmentDTO()
			.category(category)
			.name(name)
			.extension(extension)
			.mimeType(mimeType);

		ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.ifPresent(bean::setFile);

		return bean;
	}

	public static MessageAttachment toMessageAttachment(String fileName, String contentType, RenderResponse renderedContent) {
		final var bean = new MessageAttachment()
			.name(fileName)
			.contentType(contentType);

		ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.ifPresent(bean::setContent);

		return bean;
	}

	public static MessageRequest toMessageRequest(String messageId, String subject, String message, ErrandDTO errandDTO, DirectionEnum direction, String username, MessageAttachment attachment) {
		final var bean = new MessageRequest()
			.messageID(messageId)
			.direction(direction)
			.message(message)
			.subject(subject)
			.sent(ISO_OFFSET_DATE_TIME.format(getNow()))
			.username(username);

		ofNullable(errandDTO).ifPresent(errand -> bean
			.errandNumber(errand.getErrandNumber())
			.externalCaseID(errand.getExternalCaseId()));

		ofNullable(attachment)
			.ifPresent(bean::addAttachmentRequestsItem);

		return bean;
	}

	public static StatusDTO toStatus(String statusType, String description) {
		return new StatusDTO()
			.statusType(statusType)
			.dateTime(getNow())
			.description(description);
	}

	private static OffsetDateTime getNow() {
		return now(ZoneId.systemDefault());
	}
}
