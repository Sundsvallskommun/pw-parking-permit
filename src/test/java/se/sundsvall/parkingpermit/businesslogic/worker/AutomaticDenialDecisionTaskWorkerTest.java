package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.AttachmentDTO.CategoryEnum.BESLUT;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.ADMINISTRATOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum.DOCTOR;
import static generated.se.sundsvall.casedata.StakeholderDTO.TypeEnum.PERSON;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import generated.se.sundsvall.casedata.AttachmentDTO;
import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import generated.se.sundsvall.casedata.LawDTO;
import generated.se.sundsvall.casedata.StakeholderDTO;
import generated.se.sundsvall.casedata.StakeholderDTO.RolesEnum;
import generated.se.sundsvall.templating.RenderResponse;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.DenialTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
class AutomaticDenialDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Mock
	private MessagingService messagingServiceMock;

	@Mock
	private TextProvider textProviderMock;

	@Mock
	private DenialTextProperties denialTextPropertiesMock;

	@Mock
	private ErrandDTO errandMock;

	@InjectMocks
	private AutomaticDenialDecisionTaskWorker worker;

	@Captor
	private ArgumentCaptor<StakeholderDTO> stakeholderCaptor;

	@Captor
	private ArgumentCaptor<DecisionDTO> decisionCaptor;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("AutomaticDenialDecisionTask");
	}

	@Test
	void executeProcessEngineStakeholderDoesNotExist() {
		// Setup
		final var filename = "filename";
		final var description = "description";
		final var lawHeading = "lawHeading";
		final var lawSfs = "lawSfs";
		final var lawChapter = "lawChapter";
		final var lawArticle = "lawArticle";
		final var stakeholderId = RandomUtils.nextLong();
		final var stakeholderDTO = new StakeholderDTO().id(stakeholderId);
		final var output = "output";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.addStakeholderToErrand(any(), any())).thenReturn(ResponseEntity.created(URI.create("url/to/created/id/" + stakeholderId)).build());
		when(caseDataClientMock.getStakeholder(stakeholderId)).thenReturn(stakeholderDTO);
		when(messagingServiceMock.renderPdf(errandMock)).thenReturn(new RenderResponse().output(output));
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.filename()).thenReturn(filename);
		when(denialTextPropertiesMock.description()).thenReturn(description);
		when(denialTextPropertiesMock.lawHeading()).thenReturn(lawHeading);
		when(denialTextPropertiesMock.lawSfs()).thenReturn(lawSfs);
		when(denialTextPropertiesMock.lawChapter()).thenReturn(lawChapter);
		when(denialTextPropertiesMock.lawArticle()).thenReturn(lawArticle);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(ERRAND_ID), stakeholderCaptor.capture());
		verify(caseDataClientMock).getStakeholder(stakeholderId);
		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).description();
		verify(denialTextPropertiesMock).lawHeading();
		verify(denialTextPropertiesMock).lawSfs();
		verify(denialTextPropertiesMock).lawChapter();
		verify(denialTextPropertiesMock).lawArticle();
		verify(messagingServiceMock).renderPdf(errandMock);
		verify(caseDataClientMock).patchNewDecision(eq(ERRAND_ID), decisionCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		assertThat(stakeholderCaptor.getValue().getType()).isEqualTo(PERSON);
		assertThat(stakeholderCaptor.getValue().getFirstName()).isEqualTo("Process");
		assertThat(stakeholderCaptor.getValue().getLastName()).isEqualTo("Engine");
		assertThat(stakeholderCaptor.getValue().getRoles()).containsExactly(ADMINISTRATOR);
		assertThat(decisionCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionCaptor.getValue().getDecisionType()).isEqualTo(FINAL);
		assertThat(decisionCaptor.getValue().getDecisionOutcome()).isEqualTo(DISMISSAL);
		assertThat(decisionCaptor.getValue().getDescription()).isEqualTo(description);
		assertThat(decisionCaptor.getValue().getDecidedBy()).isEqualTo(stakeholderDTO);
		assertThat(decisionCaptor.getValue().getLaw())
			.extracting(
				LawDTO::getArticle,
				LawDTO::getChapter,
				LawDTO::getHeading,
				LawDTO::getSfs)
			.containsExactly(tuple(
				lawArticle,
				lawChapter,
				lawHeading,
				lawSfs));
		assertThat(decisionCaptor.getValue().getAttachments())
			.extracting(
				AttachmentDTO::getCategory,
				AttachmentDTO::getExtension,
				AttachmentDTO::getFile,
				AttachmentDTO::getName,
				AttachmentDTO::getMimeType)
			.containsExactly(tuple(
				BESLUT,
				"pdf",
				output,
				filename,
				APPLICATION_PDF_VALUE));
	}

	@Test
	void executeProcessEngineStakeholderExists() {
		// Setup
		final var filename = "filename";
		final var description = "description";
		final var lawHeading = "lawHeading";
		final var lawSfs = "lawSfs";
		final var lawChapter = "lawChapter";
		final var lawArticle = "lawArticle";
		final var stakeholderId = RandomUtils.nextLong();
		final var processEngineStakeholder = createStakeholder(stakeholderId, ADMINISTRATOR, "Process", "Engine");
		final var output = "output";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, DOCTOR, "Process", "Engine"),
			createStakeholder(null, ADMINISTRATOR, "Ssecorp", "Engine"),
			createStakeholder(null, ADMINISTRATOR, "Process", "Enigne"),
			processEngineStakeholder));

		when(messagingServiceMock.renderPdf(errandMock)).thenReturn(new RenderResponse().output(output));
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.filename()).thenReturn(filename);
		when(denialTextPropertiesMock.description()).thenReturn(description);
		when(denialTextPropertiesMock.lawHeading()).thenReturn(lawHeading);
		when(denialTextPropertiesMock.lawSfs()).thenReturn(lawSfs);
		when(denialTextPropertiesMock.lawChapter()).thenReturn(lawChapter);
		when(denialTextPropertiesMock.lawArticle()).thenReturn(lawArticle);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock, never()).addStakeholderToErrand(any(), any());
		verify(caseDataClientMock, never()).getStakeholder(any());
		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).description();
		verify(denialTextPropertiesMock).lawHeading();
		verify(denialTextPropertiesMock).lawSfs();
		verify(denialTextPropertiesMock).lawChapter();
		verify(denialTextPropertiesMock).lawArticle();
		verify(messagingServiceMock).renderPdf(errandMock);
		verify(caseDataClientMock).patchNewDecision(eq(ERRAND_ID), decisionCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		assertThat(decisionCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionCaptor.getValue().getDecisionType()).isEqualTo(FINAL);
		assertThat(decisionCaptor.getValue().getDecisionOutcome()).isEqualTo(DISMISSAL);
		assertThat(decisionCaptor.getValue().getDescription()).isEqualTo(description);
		assertThat(decisionCaptor.getValue().getDecidedBy()).isEqualTo(processEngineStakeholder);
		assertThat(decisionCaptor.getValue().getLaw())
			.extracting(
				LawDTO::getArticle,
				LawDTO::getChapter,
				LawDTO::getHeading,
				LawDTO::getSfs)
			.containsExactly(tuple(
				lawArticle,
				lawChapter,
				lawHeading,
				lawSfs));
		assertThat(decisionCaptor.getValue().getAttachments())
			.extracting(
				AttachmentDTO::getCategory,
				AttachmentDTO::getExtension,
				AttachmentDTO::getFile,
				AttachmentDTO::getName,
				AttachmentDTO::getMimeType)
			.containsExactly(tuple(
				BESLUT,
				"pdf",
				output,
				filename,
				APPLICATION_PDF_VALUE));
	}

	@Test
	void executeProcessEngineStakeholderCreationDoesNotReturnId() {
		// Mock to simulate case data not returning stakeholder id upon creation
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.addStakeholderToErrand(any(), any())).thenReturn(ResponseEntity.noContent().build());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(ERRAND_ID), any());
		verify(caseDataClientMock, never()).getStakeholder(any());
		verify(messagingServiceMock, never()).renderPdf(any());
		verify(caseDataClientMock, never()).patchNewDecision(any(), any());
		verify(externalTaskServiceMock, never()).complete(any());
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Gateway: CaseData integration did not return any location for created stakeholder");
		verify(externalTaskMock).getId();
		verifyNoInteractions(camundaClientMock, textProviderMock);
	}

	@Test
	void executeProcessEngineStakeholderCreationDoesNotReturnIdOfTypeLong() {
		// Mock to simulate case data not returning stakeholder id upon creation
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(caseDataClientMock.addStakeholderToErrand(any(), any())).thenReturn(ResponseEntity.created(URI.create("url/to/created/id/abc")).build());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(caseDataClientMock).getErrandById(ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(ERRAND_ID), any());
		verify(caseDataClientMock, never()).getStakeholder(any());
		verify(messagingServiceMock, never()).renderPdf(any());
		verify(caseDataClientMock, never()).patchNewDecision(any(), any());
		verify(externalTaskServiceMock, never()).complete(any());
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Gateway: CaseData integration did not return any location for created stakeholder");
		verify(externalTaskMock).getId();
		verifyNoInteractions(camundaClientMock, textProviderMock);

	}

	private StakeholderDTO createStakeholder(Long stakeholderId, RolesEnum role, String firstName, String lastName) {
		return new StakeholderDTO()
			.id(stakeholderId)
			.firstName(firstName)
			.lastName(lastName)
			.roles(List.of(role))
			.type(PERSON);
	}
}
