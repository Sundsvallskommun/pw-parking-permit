package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Law;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.templating.RenderResponse;
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
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.DenialTextProperties;
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
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
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.parkingpermit.Constants.CATEGORY_BESLUT;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;

@ExtendWith(MockitoExtension.class)
class AutomaticDenialDecisionTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final String ROLE_DOCTOR = "DOCTOR";

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
	private Errand errandMock;

	@Mock
	private SimplifiedServiceTextProperties simplifiedServiceTextPropertiesMock;

	@InjectMocks
	private AutomaticDenialDecisionTaskWorker worker;

	@Captor
	private ArgumentCaptor<Stakeholder> stakeholderCaptor;

	@Captor
	private ArgumentCaptor<Decision> decisionCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> mapCaptor;

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
		final var stakeholderId = new Random().nextLong();
		final var stakeholder = new Stakeholder().id(stakeholderId);
		final var output = "output";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID,NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(caseDataClientMock.addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any())).thenReturn(ResponseEntity.created(URI.create("url/to/created/id/" + stakeholderId)).build());
		when(caseDataClientMock.getStakeholder(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, stakeholderId)).thenReturn(stakeholder);
		when(messagingServiceMock.renderPdf(MUNICIPALITY_ID, errandMock)).thenReturn(new RenderResponse().output(output));
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.filename()).thenReturn(filename);
		when(denialTextPropertiesMock.description()).thenReturn(description);
		when(denialTextPropertiesMock.lawHeading()).thenReturn(lawHeading);
		when(denialTextPropertiesMock.lawSfs()).thenReturn(lawSfs);
		when(denialTextPropertiesMock.lawChapter()).thenReturn(lawChapter);
		when(denialTextPropertiesMock.lawArticle()).thenReturn(lawArticle);
		when(simplifiedServiceTextPropertiesMock.delay()).thenReturn("P1D");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), stakeholderCaptor.capture());
		verify(caseDataClientMock).getStakeholder(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, stakeholderId);
		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).description();
		verify(denialTextPropertiesMock).lawHeading();
		verify(denialTextPropertiesMock).lawSfs();
		verify(denialTextPropertiesMock).lawChapter();
		verify(denialTextPropertiesMock).lawArticle();
		verify(messagingServiceMock).renderPdf(MUNICIPALITY_ID, errandMock);
		verify(caseDataClientMock).patchNewDecision(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		verify(externalTaskServiceMock).complete(any(ExternalTask.class), mapCaptor.capture());
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		assertThat(mapCaptor.getValue()).containsOnlyKeys(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE);
		assertThat(mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE)).isInstanceOf(Date.class);
		assertThat(((Date) mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE))).isCloseTo(now().plusDays(1).toInstant(), 2000);

		assertThat(stakeholderCaptor.getValue())
			.extracting(Stakeholder::getType, Stakeholder::getFirstName, Stakeholder::getLastName, Stakeholder::getRoles)
			.containsExactly(PERSON, "Process", "Engine", List.of(ROLE_ADMINISTRATOR));

		assertThat(decisionCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionCaptor.getValue())
			.extracting(Decision::getDecisionType, Decision::getDecisionOutcome, Decision::getDescription, Decision::getDecidedBy)
			.containsExactly(FINAL, DISMISSAL, description, stakeholder);

		assertThat(decisionCaptor.getValue().getLaw())
			.extracting(
				Law::getArticle,
				Law::getChapter,
				Law::getHeading,
				Law::getSfs)
			.containsExactly(tuple(
				lawArticle,
				lawChapter,
				lawHeading,
				lawSfs));
		assertThat(decisionCaptor.getValue().getAttachments())
			.extracting(
				Attachment::getCategory,
				Attachment::getExtension,
				Attachment::getFile,
				Attachment::getName,
				Attachment::getMimeType)
			.containsExactly(tuple(
				CATEGORY_BESLUT,
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
		final var stakeholderId = new Random().nextLong();
		final var processEngineStakeholder = createStakeholder(stakeholderId, ROLE_ADMINISTRATOR, "Process", "Engine");
		final var output = "output";

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(errandMock.getStakeholders()).thenReturn(List.of(
			createStakeholder(null, ROLE_DOCTOR, "Process", "Engine"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Ssecorp", "Engine"),
			createStakeholder(null, ROLE_ADMINISTRATOR, "Process", "Enigne"),
			processEngineStakeholder));

		when(messagingServiceMock.renderPdf(MUNICIPALITY_ID, errandMock)).thenReturn(new RenderResponse().output(output));
		when(textProviderMock.getDenialTexts()).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.filename()).thenReturn(filename);
		when(denialTextPropertiesMock.description()).thenReturn(description);
		when(denialTextPropertiesMock.lawHeading()).thenReturn(lawHeading);
		when(denialTextPropertiesMock.lawSfs()).thenReturn(lawSfs);
		when(denialTextPropertiesMock.lawChapter()).thenReturn(lawChapter);
		when(denialTextPropertiesMock.lawArticle()).thenReturn(lawArticle);
		when(simplifiedServiceTextPropertiesMock.delay()).thenReturn("P1D");

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock, never()).addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any());
		verify(caseDataClientMock, never()).getStakeholder(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(denialTextPropertiesMock).filename();
		verify(denialTextPropertiesMock).description();
		verify(denialTextPropertiesMock).lawHeading();
		verify(denialTextPropertiesMock).lawSfs();
		verify(denialTextPropertiesMock).lawChapter();
		verify(denialTextPropertiesMock).lawArticle();
		verify(messagingServiceMock).renderPdf(MUNICIPALITY_ID, errandMock);
		verify(caseDataClientMock).patchNewDecision(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		verify(externalTaskServiceMock).complete(any(ExternalTask.class), mapCaptor.capture());
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		assertThat(mapCaptor.getValue()).containsOnlyKeys(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE);
		assertThat(((Date) mapCaptor.getValue().get(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE))).isCloseTo(now().plusDays(1).toInstant(),  2000);
		assertThat(decisionCaptor.getValue().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(decisionCaptor.getValue().getDecisionType()).isEqualTo(FINAL);
		assertThat(decisionCaptor.getValue().getDecisionOutcome()).isEqualTo(DISMISSAL);
		assertThat(decisionCaptor.getValue().getDescription()).isEqualTo(description);
		assertThat(decisionCaptor.getValue().getDecidedBy()).isEqualTo(processEngineStakeholder);
		assertThat(decisionCaptor.getValue().getLaw())
			.extracting(
				Law::getArticle,
				Law::getChapter,
				Law::getHeading,
				Law::getSfs)
			.containsExactly(tuple(
				lawArticle,
				lawChapter,
				lawHeading,
				lawSfs));
		assertThat(decisionCaptor.getValue().getAttachments())
			.extracting(
				Attachment::getCategory,
				Attachment::getExtension,
				Attachment::getFile,
				Attachment::getName,
				Attachment::getMimeType)
			.containsExactly(tuple(
				CATEGORY_BESLUT,
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(caseDataClientMock.addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any())).thenReturn(ResponseEntity.noContent().build());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(caseDataClientMock, never()).getStakeholder(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(messagingServiceMock, never()).renderPdf(eq(MUNICIPALITY_ID), any());
		verify(caseDataClientMock, never()).patchNewDecision(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any());
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
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		when(errandMock.getNamespace()).thenReturn(NAMESPACE);
		when(caseDataClientMock.addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any())).thenReturn(ResponseEntity.created(URI.create("url/to/created/id/abc")).build());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(caseDataClientMock).addStakeholderToErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(caseDataClientMock, never()).getStakeholder(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(messagingServiceMock, never()).renderPdf(eq(MUNICIPALITY_ID), any());
		verify(caseDataClientMock, never()).patchNewDecision(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any());
		verify(externalTaskServiceMock, never()).complete(any());
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Bad Gateway: CaseData integration did not return any location for created stakeholder");
		verify(externalTaskMock).getId();
		verifyNoInteractions(camundaClientMock, textProviderMock);

	}

	private Stakeholder createStakeholder(Long stakeholderId, String role, String firstName, String lastName) {
		return new Stakeholder()
			.id(stakeholderId)
			.firstName(firstName)
			.lastName(lastName)
			.roles(List.of(role))
			.type(PERSON);
	}
}
