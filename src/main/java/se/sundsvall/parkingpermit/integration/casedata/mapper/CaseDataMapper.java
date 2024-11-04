package se.sundsvall.parkingpermit.integration.casedata.mapper;

import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.Decision.DecisionTypeEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Law;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.MessageRequest.DirectionEnum;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Stakeholder.TypeEnum;
import generated.se.sundsvall.casedata.Status;
import generated.se.sundsvall.templating.RenderResponse;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;

public class CaseDataMapper {

	private CaseDataMapper() {}

	public static PatchErrand toPatchErrand(String externalCaseId, String phase, String phaseStatus, String phaseAction, String displayPhase) {
		final var patchErrand =  toPatchErrand(externalCaseId, phase, phaseStatus, phaseAction);
		return displayPhase != null ? patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).addValuesItem(displayPhase)) :
			patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE));
	}

	public static PatchErrand toPatchErrand(String externalCaseId, String phase, String phaseStatus, String phaseAction) {
		final var patchErrand =  new PatchErrand()
				.externalCaseId(externalCaseId)
				.phase(phase)
				.facilities(null);

		Optional.ofNullable(phaseStatus).ifPresentOrElse(aPhaseStatus -> patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS).addValuesItem(aPhaseStatus)),
				() -> patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS)));
		Optional.ofNullable(phaseAction).ifPresentOrElse(aPhaseAction -> patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).addValuesItem(aPhaseAction)),
			() -> patchErrand.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION)));

		return patchErrand;
	}

	public static Stakeholder toStakeholder(String role, TypeEnum type, String firstName, String lastName) {
		final var bean = new Stakeholder()
			.firstName(firstName)
			.lastName(lastName)
			.type(type);

		ofNullable(role).ifPresent(bean::addRolesItem);

		return bean;
	}

	public static Decision toDecision(DecisionTypeEnum decisionType, DecisionOutcomeEnum decisionOutcome, String description) {
		return new Decision()
			.created(getNow())
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcome)
			.description(description);
	}

	public static Law toLaw(String heading, String sfs, String chapter, String article) {
		return new Law()
			.heading(heading)
			.sfs(sfs)
			.chapter(chapter)
			.article(article);
	}

	public static Attachment toAttachment(String category, String name, String extension, String mimeType, RenderResponse renderedContent) {
		final var bean = new Attachment()
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

	public static MessageRequest toMessageRequest(String messageId, String subject, String message, Errand errand, DirectionEnum direction, String username, MessageAttachment attachment) {
		final var bean = new MessageRequest()
			.messageId(messageId)
			.direction(direction)
			.message(message)
			.subject(subject)
			.sent(ISO_OFFSET_DATE_TIME.format(getNow()))
			.username(username);

		ofNullable(errand).ifPresent(theErrand -> bean
			.errandNumber(theErrand.getErrandNumber())
			.externalCaseId(theErrand.getExternalCaseId()));

		ofNullable(attachment)
			.ifPresent(bean::addAttachmentRequestsItem);

		return bean;
	}

	public static Status toStatus(String statusType, String description) {
		return new Status()
			.statusType(statusType)
			.dateTime(getNow())
			.description(description);
	}

	private static OffsetDateTime getNow() {
		return now(ZoneId.systemDefault());
	}
}
