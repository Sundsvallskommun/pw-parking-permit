package se.sundsvall.parkingpermit.integration.casedata.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Law;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.MessageRequest.DirectionEnum;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Stakeholder.TypeEnum;
import generated.se.sundsvall.templating.RenderResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseDataMapperTest {

	@Test
	void toDecisionBasedOnNullValues() {
		final var bean = CaseDataMapper.toDecision(null, null, null);

		assertThat(bean).hasAllNullFieldsOrPropertiesExcept("attachments", "created", "extraParameters", "law");
		assertThat(bean.getAttachments()).isNullOrEmpty();
		assertThat(bean.getExtraParameters()).isNullOrEmpty();
		assertThat(bean.getLaw()).isNullOrEmpty();
		assertThat(bean.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toDecision() {
		final var bean = CaseDataMapper.toDecision(Decision.DecisionTypeEnum.FINAL, Decision.DecisionOutcomeEnum.REJECTION, "Description");

		assertThat(bean).hasAllNullFieldsOrPropertiesExcept("attachments", "created", "created", "decisionType", "decisionOutcome", "description", "extraParameters", "law");
		assertThat(bean.getAttachments()).isNullOrEmpty();
		assertThat(bean.getExtraParameters()).isNullOrEmpty();
		assertThat(bean.getLaw()).isNullOrEmpty();

		assertThat(bean.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(bean.getDecisionType()).isEqualTo(Decision.DecisionTypeEnum.FINAL);
		assertThat(bean.getDecisionOutcome()).isEqualTo(Decision.DecisionOutcomeEnum.REJECTION);
		assertThat(bean.getDescription()).isEqualTo("Description");
	}

	@Test
	void toStatusFromTypeAndDescription() {
		final var statusType = "statusType";
		final var description = "description";
		final var bean = CaseDataMapper.toStatus(statusType, description);

		assertThat(bean.getStatusType()).isEqualTo(statusType);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toMessageRequestWithNullAsParameters() {
		final var bean = CaseDataMapper.toMessageRequest(null, null, null, null, null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("sent", "attachments", "emailHeaders", "recipients");
		assertThat(OffsetDateTime.parse(bean.getSent())).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toMessageRequest() {
		final var externalCaseId = "externalCaseId";
		final var messageId = "messageId";
		final var subject = "subject";
		final var message = "message";
		final var errand = new Errand().externalCaseId(externalCaseId);
		final var direction = DirectionEnum.OUTBOUND;
		final var username = "username";
		final var attachment = new MessageAttachment();

		final var bean = CaseDataMapper.toMessageRequest(messageId, subject, message, errand, direction, username, attachment);

		assertThat(bean).isNotNull()
			.extracting(
				MessageRequest::getAttachments,
				MessageRequest::getDirection,
				MessageRequest::getEmail,
				MessageRequest::getExternalCaseId,
				MessageRequest::getFamilyId,
				MessageRequest::getFirstName,
				MessageRequest::getLastName,
				MessageRequest::getMessage,
				MessageRequest::getMessageId,
				MessageRequest::getSubject,
				MessageRequest::getUserId,
				MessageRequest::getUsername)
			.containsExactly(
				List.of(attachment),
				direction,
				null,
				externalCaseId,
				null,
				null,
				null,
				message,
				messageId,
				subject,
				null,
				username);

		assertThat(OffsetDateTime.parse(bean.getSent())).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toMessageAttachmentWithNullAsParameters() {
		final var bean = CaseDataMapper.toMessageAttachment(null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrProperties();
	}

	@Test
	void toMessageAttachment() {
		final var filename = "filename";
		final var contentType = "contentType";
		final var output = "ZmlsZW91dHB1dCBhcyBiYXNlNjQgc3RyaW5n";
		final var content = new RenderResponse().output(output);

		final var bean = CaseDataMapper.toMessageAttachment(filename, contentType, content);

		assertThat(bean).isNotNull()
			.extracting(
				MessageAttachment::getContent,
				MessageAttachment::getContentType,
				MessageAttachment::getName)
			.containsExactly(
				output,
				contentType,
				filename);
	}

	@Test
	void toPatchErrandWithNullAsParameters() {
		final var bean = CaseDataMapper.toPatchErrand(null, null, null, "phaseAction", null, null);

		final var expectedExtraParameters = List.of(
			new ExtraParameter("process.phaseStatus").values(emptyList()),
			new ExtraParameter("process.phaseAction").values(List.of("phaseAction")),
			new ExtraParameter("process.displayPhase").values(emptyList()));
		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("extraParameters", "relatesTo", "labels", "facilities")
			.extracting(PatchErrand::getFacilities,
				PatchErrand::getExtraParameters)
			.containsExactly(emptyList(), expectedExtraParameters);
	}

	@Test
	void toPatchErrandWithPhaseActionNull() {
		final var extraParameters = List.of(new ExtraParameter("key").values(List.of("value")));
		assertThatThrownBy(() -> CaseDataMapper.toPatchErrand("externalCaseId", "phase", "phaseStatus", null, "dispayPhase", extraParameters))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("phaseAction cannot be null");
	}

	@Test
	void toPatchErrand() {
		final var externalCaseId = "externalCaseId";
		final var phase = "phase";
		final var phaseStatus = "phaseStatus";
		final var phaseAction = "phaseAction";
		final var displayPhase = "displayPhase";
		final var keyPhaseAction = "process.phaseAction";
		final var keyPhaseStatus = "process.phaseStatus";
		final var keyDisplayPhase = "process.displayPhase";
		final var keyOther = "key";
		final var valueOther = "value";
		final var extraParameters = List.of(new ExtraParameter(keyOther).addValuesItem(valueOther), new ExtraParameter(keyPhaseAction).addValuesItem("existingPhaseAction"));

		final var bean = CaseDataMapper.toPatchErrand(externalCaseId, phase, phaseStatus, phaseAction, displayPhase, extraParameters);

		assertThat(bean).isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase", "extraParameters", "relatesTo", "labels", "facilities")
			.extracting(
				PatchErrand::getExternalCaseId,
				PatchErrand::getPhase,
				PatchErrand::getFacilities,
				PatchErrand::getExtraParameters)
			.containsExactly(
				externalCaseId,
				phase,
				emptyList(),
				List.of(
					new ExtraParameter(keyOther).addValuesItem(valueOther),
					new ExtraParameter(keyPhaseStatus).addValuesItem(phaseStatus),
					new ExtraParameter(keyPhaseAction).addValuesItem(phaseAction),
					new ExtraParameter(keyDisplayPhase).addValuesItem(displayPhase)));
	}

	@Test
	void toPatchErrandWithoutDisplayPhase() {
		final var externalCaseId = "externalCaseId";
		final var phase = "phase";
		final var phaseStatus = "phaseStatus";
		final var phaseAction = "phaseAction";
		final var keyPhaseAction = "process.phaseAction";
		final var extraParameters = List.of(new ExtraParameter("key").addValuesItem("value"), new ExtraParameter(keyPhaseAction).addValuesItem("existingPhaseAction"));

		final var bean = CaseDataMapper.toPatchErrand(externalCaseId, phase, phaseStatus, phaseAction, extraParameters);

		assertThat(bean).isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase", "extraParameters", "relatesTo", "labels", "facilities")
			.extracting(
				PatchErrand::getExternalCaseId,
				PatchErrand::getPhase,
				PatchErrand::getFacilities,
				PatchErrand::getExtraParameters)
			.containsExactly(
				externalCaseId,
				phase,
				emptyList(),
				List.of(new ExtraParameter("key").addValuesItem("value"),
					new ExtraParameter("process.phaseStatus").addValuesItem(phaseStatus),
					new ExtraParameter(keyPhaseAction).addValuesItem(phaseAction)));
	}

	@Test
	void toLawWithNullAsParameters() {
		final var bean = CaseDataMapper.toLaw(null, null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrProperties();
	}

	@Test
	void toLaw() {
		final var heading = "heading";
		final var sfs = "sfs";
		final var chapter = "chapter";
		final var article = "article";

		final var bean = CaseDataMapper.toLaw(heading, sfs, chapter, article);

		assertThat(bean).isNotNull()
			.extracting(
				Law::getArticle,
				Law::getChapter,
				Law::getHeading,
				Law::getSfs)
			.containsExactly(
				article,
				chapter,
				heading,
				sfs);
	}

	@Test
	void toAttachmentWithNullAsParameters() {
		final var bean = CaseDataMapper.toAttachment(null, null, null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("extraParameters")
			.extracting(Attachment::getExtraParameters)
			.isEqualTo(emptyMap());
	}

	@Test
	void toAttachment() {
		final var category = "POLICE_REPORT";
		final var name = "name";
		final var extension = "extension";
		final var mimeType = "mimeType";
		final var output = "ZmlsZW91dHB1dCBhcyBiYXNlNjQgc3RyaW5n";
		final var renderedContent = new RenderResponse().output(output);

		final var bean = CaseDataMapper.toAttachment(category, name, extension, mimeType, renderedContent);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("category", "name", "extension", "mimeType", "file", "extraParameters")
			.extracting(
				Attachment::getCategory,
				Attachment::getName,
				Attachment::getExtension,
				Attachment::getMimeType,
				Attachment::getFile,
				Attachment::getExtraParameters)
			.containsExactly(
				category,
				name,
				extension,
				mimeType,
				output,
				emptyMap());
	}

	@Test
	void toStakeholderWithNullParameters() {
		final var bean = CaseDataMapper.toStakeholder(null, null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("extraParameters", "roles", "addresses", "contactInformation")
			.extracting(
				Stakeholder::getExtraParameters,
				Stakeholder::getRoles)
			.containsExactly(
				emptyMap(),
				emptyList());
	}

	@Test
	void toStakeholder() {
		final var role = "OPERATOR";
		final var type = TypeEnum.ORGANIZATION;
		final var firstName = "firstName";
		final var lastName = "lastName";

		final var bean = CaseDataMapper.toStakeholder(role, type, firstName, lastName);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("roles", "type", "firstName", "lastName", "extraParameters", "addresses", "contactInformation")
			.extracting(
				Stakeholder::getExtraParameters,
				Stakeholder::getFirstName,
				Stakeholder::getLastName,
				Stakeholder::getRoles,
				Stakeholder::getType)
			.containsExactly(
				emptyMap(),
				firstName,
				lastName,
				List.of(role),
				type);
	}
}
