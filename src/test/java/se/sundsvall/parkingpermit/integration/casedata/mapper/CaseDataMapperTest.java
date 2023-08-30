package se.sundsvall.parkingpermit.integration.casedata.mapper;

import generated.se.sundsvall.casedata.AttachmentDTO;
import generated.se.sundsvall.casedata.AttachmentDTO.CategoryEnum;
import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.LawDTO;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.MessageRequest.DirectionEnum;
import generated.se.sundsvall.casedata.PatchErrandDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum;
import generated.se.sundsvall.templating.RenderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class CaseDataMapperTest {
	public static final String STATUS_CASE_PROCESSED = "Under utredning";

	@Mock
	private DecisionDTO decisionMock;

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
		final var bean = CaseDataMapper.toDecision(DecisionDTO.DecisionTypeEnum.FINAL, DecisionDTO.DecisionOutcomeEnum.REJECTION, "Description");

		assertThat(bean).hasAllNullFieldsOrPropertiesExcept("attachments", "created", "created", "decisionType", "decisionOutcome", "description", "extraParameters", "law");
		assertThat(bean.getAttachments()).isNullOrEmpty();
		assertThat(bean.getExtraParameters()).isNullOrEmpty();
		assertThat(bean.getLaw()).isNullOrEmpty();

		assertThat(bean.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
		assertThat(bean.getDecisionType()).isEqualTo(DecisionDTO.DecisionTypeEnum.FINAL);
		assertThat(bean.getDecisionOutcome()).isEqualTo(DecisionDTO.DecisionOutcomeEnum.REJECTION);
		assertThat(bean.getDescription()).isEqualTo("Description");
	}

	@Test
	void toStatusFromTypeAndDescription() {
		final var statusType = "statusType";
		final var description = "description";
		final var bean = CaseDataMapper.toStatus(statusType, description);

		assertThat(bean.getStatusType()).isEqualTo(statusType);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getDateTime()).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toMessageRequestWithNullAsParameters() {
		final var bean = CaseDataMapper.toMessageRequest(null, null, null, null, null, null, null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("sent");
		assertThat(OffsetDateTime.parse(bean.getSent())).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toMessageRequest() {
		final var errandNumber = "errandNumber";
		final var externalCaseId = "externalCaseId";
		final var messageId = "messageId";
		final var subject = "subject";
		final var message = "message";
		final var errandDTO = new ErrandDTO().errandNumber(errandNumber).externalCaseId(externalCaseId);
		final var direction = DirectionEnum.OUTBOUND;
		final var username = "username";
		final var attachment = new MessageAttachment();

		final var bean = CaseDataMapper.toMessageRequest(messageId, subject, message, errandDTO, direction, username, attachment);

		assertThat(bean).isNotNull()
			.extracting(
				MessageRequest::getAttachmentRequests,
				MessageRequest::getDirection,
				MessageRequest::getEmail,
				MessageRequest::getErrandNumber,
				MessageRequest::getExternalCaseID,
				MessageRequest::getFamilyID,
				MessageRequest::getFirstName,
				MessageRequest::getLastName,
				MessageRequest::getMessage,
				MessageRequest::getMessageID,
				MessageRequest::getSubject,
				MessageRequest::getUserID,
				MessageRequest::getUsername)
			.containsExactly(
				List.of(attachment),
				direction,
				null,
				errandNumber,
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
		final var bean = CaseDataMapper.toPatchErrand(null, null, null, null);

		final var expectedExtraParameters = new HashMap<String, String>();
		expectedExtraParameters.put("process.phaseStatus", null);
		expectedExtraParameters.put("process.phaseAction", null);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("extraParameters")
			.extracting(PatchErrandDTO::getExtraParameters)
			.isEqualTo(expectedExtraParameters);
	}

	@Test
	void toPatchErrand() {
		final var externalCaseId = "externalCaseId";
		final var phase = "phase";
		final var phaseStatus = "phaseStatus";
		final var phaseAction = "phaseAction";

		final var bean = CaseDataMapper.toPatchErrand(externalCaseId, phase, phaseStatus, phaseAction);

		assertThat(bean).isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("externalCaseId", "phase", "extraParameters")
			.extracting(
				PatchErrandDTO::getExternalCaseId,
				PatchErrandDTO::getPhase,
				PatchErrandDTO::getExtraParameters)
			.containsExactly(
				externalCaseId,
				phase,
				Map.of(
					"process.phaseStatus", phaseStatus,
					"process.phaseAction", phaseAction));
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
				LawDTO::getArticle,
				LawDTO::getChapter,
				LawDTO::getHeading,
				LawDTO::getSfs)
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
			.extracting(AttachmentDTO::getExtraParameters)
			.isEqualTo(emptyMap());
	}

	@Test
	void toAttachment() {
		final var category = CategoryEnum.POLICE_REPORT;
		final var name = "name";
		final var extension = "extension";
		final var mimeType = "mimeType";
		final var output = "ZmlsZW91dHB1dCBhcyBiYXNlNjQgc3RyaW5n";
		final var renderedContent = new RenderResponse().output(output);

		final var bean = CaseDataMapper.toAttachment(category, name, extension, mimeType, renderedContent);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("category", "name", "extension", "mimeType", "_file", "extraParameters")
			.extracting(
				AttachmentDTO::getCategory,
				AttachmentDTO::getName,
				AttachmentDTO::getExtension,
				AttachmentDTO::getMimeType,
				AttachmentDTO::getFile,
				AttachmentDTO::getExtraParameters)
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

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("extraParameters", "roles")
			.extracting(
				StakeholderDTO::getExtraParameters,
				StakeholderDTO::getRoles)
			.containsExactly(
				emptyMap(),
				emptyList());
	}

	@Test
	void toStakeholder() {
		final var role = RolesEnum.OPERATOR;
		final var type = TypeEnum.ORGANIZATION;
		final var firstName = "firstName";
		final var lastName = "lastName";

		final var bean = CaseDataMapper.toStakeholder(role, type, firstName, lastName);

		assertThat(bean).isNotNull().hasAllNullFieldsOrPropertiesExcept("roles", "type", "firstName", "lastName", "extraParameters")
			.extracting(
				StakeholderDTO::getExtraParameters,
				StakeholderDTO::getFirstName,
				StakeholderDTO::getLastName,
				StakeholderDTO::getRoles,
				StakeholderDTO::getType)
			.containsExactly(
				emptyMap(),
				firstName,
				lastName,
				List.of(role),
				type);
	}
}
