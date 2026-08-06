package se.sundsvall.parkingpermit.integration.casedata.mapper;

import feign.form.FormData;
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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.ObjectMapper;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_STATUS;

public class CaseDataMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private CaseDataMapper() {}

	public static List<ExtraParameter> toExtraParameterList(final String nullablePhaseStatus, final String nullablePhaseAction, final String nullableDisplayPhase) {
		final var extraParameters = toExtraParameterList(nullablePhaseStatus, nullablePhaseAction);

		// DisplayStatus is set if provided, or to an empty list if absent
		ofNullable(nullableDisplayPhase).ifPresentOrElse(
			displayPhase -> extraParameters.add(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).values(List.of(displayPhase))),
			() -> extraParameters.add(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).values(emptyList())));

		return extraParameters;
	}

	public static List<ExtraParameter> toExtraParameterList(final String nullablePhaseStatus, final String nullablePhaseAction) {
		final var extraParameters = new ArrayList<ExtraParameter>();

		// PhaseStatus is set if provided, or to an empty list if absent
		ofNullable(nullablePhaseStatus).ifPresentOrElse(
			phaseStatus -> extraParameters.add(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS).values(List.of(phaseStatus))),
			() -> extraParameters.add(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS).values(emptyList())));

		// PhaseAction cannot be null since that could erase action when it is "AUTOMATIC"
		ofNullable(nullablePhaseAction).ifPresentOrElse(phaseAction -> extraParameters.add(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).values(List.of(phaseAction))),
			() -> { throw new IllegalArgumentException("phaseAction cannot be null"); });

		return extraParameters;
	}

	public static PatchErrand toPatchErrand(final String externalCaseId, final String phase) {
		return new PatchErrand()
			.externalCaseId(externalCaseId)
			.phase(phase);
	}

	public static Stakeholder toStakeholder(final String role, final TypeEnum type, final String firstName, final String lastName) {
		final var bean = new Stakeholder()
			.firstName(firstName)
			.lastName(lastName)
			.type(type);

		ofNullable(role).ifPresent(bean::addRolesItem);

		return bean;
	}

	public static Decision toDecision(final DecisionTypeEnum decisionType, final DecisionOutcomeEnum decisionOutcome, final String description) {
		return new Decision()
			.created(getNow())
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcome)
			.description(description);
	}

	public static Law toLaw(final String heading, final String sfs, final String chapter, final String article) {
		return new Law()
			.heading(heading)
			.sfs(sfs)
			.chapter(chapter)
			.article(article);
	}

	/**
	 * Creates the attachment metadata. Since CaseData major version 13 the binary content is no longer carried by the
	 * metadata but uploaded as a separate multipart part, see
	 * {@link #toAttachmentFilePart(String, String, RenderResponse)}.
	 */
	public static Attachment toAttachment(final String category, final String name, final String extension, final String mimeType) {
		return new Attachment()
			.category(category)
			.name(name)
			.extension(extension)
			.mimeType(mimeType);
	}

	/**
	 * Creates the 'attachment' multipart part, holding the attachment metadata as JSON.
	 */
	public static FormData toAttachmentMetadataPart(final Attachment attachment) {
		return new FormData(APPLICATION_JSON_VALUE, null, OBJECT_MAPPER.writeValueAsBytes(attachment));
	}

	/**
	 * Creates the 'file' multipart part, holding the decoded binary content of the rendered document. An absent or blank
	 * rendering yields an empty part rather than a null one, since a missing part would be rejected by CaseData with a
	 * less telling error than an empty file.
	 */
	public static FormData toAttachmentFilePart(final String fileName, final String mimeType, final RenderResponse renderedContent) {
		final var content = ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.filter(StringUtils::isNotBlank)
			.map(output -> Base64.getDecoder().decode(output.getBytes(StandardCharsets.UTF_8)))
			.orElseGet(() -> new byte[0]);

		return new FormData(mimeType, fileName, content);
	}

	public static MessageAttachment toMessageAttachment(final String fileName, final String contentType, final RenderResponse renderedContent) {
		final var bean = new MessageAttachment()
			.name(fileName)
			.contentType(contentType);

		ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.ifPresent(bean::setContent);

		return bean;
	}

	public static MessageRequest toMessageRequest(final String messageId, final String subject, final String message, final Errand errand, final DirectionEnum direction, final String username, final MessageAttachment attachment) {
		final var bean = new MessageRequest()
			.messageId(messageId)
			.direction(direction)
			.message(message)
			.subject(subject)
			.sent(ISO_OFFSET_DATE_TIME.format(getNow()))
			.username(username);

		ofNullable(errand).ifPresent(theErrand -> bean
			.externalCaseId(theErrand.getExternalCaseId()));

		ofNullable(attachment)
			.ifPresent(bean::addAttachmentsItem);

		return bean;
	}

	public static Status toStatus(final String statusType, final String description) {
		return new Status()
			.statusType(statusType)
			.created(getNow())
			.description(description);
	}

	private static OffsetDateTime getNow() {
		return now(ZoneId.systemDefault());
	}
}
