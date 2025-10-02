package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.Constants.ROLE_APPLICANT;
import static se.sundsvall.parkingpermit.Constants.SM_CATEGORY_URBAN_DEVELOPMENT;
import static se.sundsvall.parkingpermit.Constants.SM_LABEL_CARD_MANAGEMENT;
import static se.sundsvall.parkingpermit.Constants.SM_LABEL_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.SM_LABEL_URBAN_DEVELOPMENT;
import static se.sundsvall.parkingpermit.Constants.SM_NAMESPACE_CONTACTANGE;
import static se.sundsvall.parkingpermit.Constants.SM_TYPE_PARKING_PERMIT;

import generated.se.sundsvall.casedata.Address;
import generated.se.sundsvall.casedata.ContactInformation;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.supportmanagement.Classification;
import generated.se.sundsvall.supportmanagement.ContactChannel;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.service.SupportManagementService;
import se.sundsvall.parkingpermit.util.ApprovalTextProperties;
import se.sundsvall.parkingpermit.util.CommonTextProperties;
import se.sundsvall.parkingpermit.util.DenialTextProperties;
import se.sundsvall.parkingpermit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
class DecisionHandlingTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String ERRAND_NUMBER = "ErrandNumber";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private Errand errandMock;

	@Mock
	private generated.se.sundsvall.supportmanagement.Errand supportManagementErrandMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CommonTextProperties commonTextPropertiesMock;

	@Mock
	private ApprovalTextProperties approvalTextPropertiesMock;

	@Mock
	private DenialTextProperties denialTextPropertiesMock;

	@Mock
	private TextProvider textProviderMock;

	@Mock
	private MessagingService messagingServiceMock;

	@Mock
	private SupportManagementService supportManagementServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@InjectMocks
	private DecisionHandlingTaskWorker worker;

	@Captor
	private ArgumentCaptor<Map<String, Object>> mapCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.supportmanagement.Errand> supportManagementErrandCaptor;

	@Test
	void executeWhenDecisionIsApprovedAndSendDigital() {

		// Arrange
		final var pdf = new RenderResponse();
		final var messageUUID = UUID.randomUUID();
		final var smErrandId = UUID.randomUUID().toString();
		final var extraParameters = List.of(new ExtraParameter().key(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).values(List.of("DRIVER")));
		final var templateIdentifier = "sbk.rph.decision.driver.approval";

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getErrandNumber()).thenReturn(ERRAND_NUMBER);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(APPROVAL)));
		when(errandMock.getStakeholders()).thenReturn(createApplicantAndAdministratorStakeholder());
		when(errandMock.getExtraParameters()).thenReturn(extraParameters);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(commonTextPropertiesMock.getSendDigitalMail()).thenReturn(true);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier)).thenReturn(pdf);
		when(messagingServiceMock.sendDecisionMessage(MUNICIPALITY_ID, errandMock, pdf, true)).thenReturn(messageUUID);
		when(supportManagementServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), supportManagementErrandCaptor.capture())).thenReturn(Optional.of(smErrandId));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(any(ExternalTask.class), mapCaptor.capture());
		assertThat(mapCaptor.getValue()).containsEntry(CAMUNDA_VARIABLE_MESSAGE_ID, messageUUID.toString());
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(messagingServiceMock).renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier);
		verify(messagingServiceMock).sendDecisionMessage(MUNICIPALITY_ID, errandMock, pdf, true);
		assertThat(supportManagementErrandCaptor.getValue())
			.extracting(generated.se.sundsvall.supportmanagement.Errand::getStatus,
				generated.se.sundsvall.supportmanagement.Errand::getTitle,
				generated.se.sundsvall.supportmanagement.Errand::getDescription,
				generated.se.sundsvall.supportmanagement.Errand::getClassification,
				generated.se.sundsvall.supportmanagement.Errand::getLabels,
				generated.se.sundsvall.supportmanagement.Errand::getChannel,
				generated.se.sundsvall.supportmanagement.Errand::getStakeholders)
			.containsExactly("NEW",
				"Korthantering av parkeringstillstånd",
				"Hantering av kortet gällande parkeringstillstånd ska ske av kontaktcenter: " + ERRAND_NUMBER,
				new Classification().category(SM_CATEGORY_URBAN_DEVELOPMENT).type(SM_TYPE_PARKING_PERMIT),
				List.of(SM_LABEL_URBAN_DEVELOPMENT, SM_LABEL_PARKING_PERMIT, SM_LABEL_CARD_MANAGEMENT),
				null,
				List.of(new generated.se.sundsvall.supportmanagement.Stakeholder()
					.externalId("personId")
					.externalIdType("PRIVATE")
					.role("CONTACT")
					.city("Ankeborg")
					.firstName("Kalle")
					.lastName("Anka")
					.address("Storgatan 1")
					.careOf("c/o Anka")
					.zipCode("12345")
					.country("Sverige")
					.contactChannels(List.of(new ContactChannel().type("Email").value("kalle.anka@ange.se"),
						new ContactChannel().type("Phone").value("0701234567")))));

		verifyNoMoreInteractions(camundaClientMock, messagingServiceMock, supportManagementServiceMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenDecisionIsApprovedAndNotSendDigital() {

		// Arrange
		final var pdf = new RenderResponse().output("pdf");
		final var smErrandId = UUID.randomUUID().toString();
		final var fileName = "decision.pdf";
		final var extraParameters = List.of(new ExtraParameter().key(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).values(List.of("PASSENGER")));
		final var templateIdentifier = "sbk.rph.decision.passenger.approval";

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(textProviderMock.getApprovalTexts(MUNICIPALITY_ID)).thenReturn(approvalTextPropertiesMock);
		when(approvalTextPropertiesMock.getFilename()).thenReturn(fileName);
		when(commonTextPropertiesMock.getSendDigitalMail()).thenReturn(false);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(APPROVAL)));
		when(errandMock.getStakeholders()).thenReturn(createApplicantAndAdministratorStakeholder());
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getExtraParameters()).thenReturn(extraParameters);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier)).thenReturn(pdf);
		when(supportManagementServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), any())).thenReturn(Optional.of(smErrandId));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(any(ExternalTask.class));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(supportManagementServiceMock, times(2)).createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), any(generated.se.sundsvall.supportmanagement.Errand.class));
		verify(supportManagementServiceMock).createAttachment(MUNICIPALITY_ID, SM_NAMESPACE_CONTACTANGE, smErrandId, fileName, pdf.getOutput());
		verifyNoMoreInteractions(camundaClientMock, messagingServiceMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeWhenDecisionIsDenialAndExceptionSendingDigitalMail() {

		// Arrange
		final var thrownException = new RuntimeException("TestException", new RestException("message", "type", 1));
		final var pdf = new RenderResponse().output("pdf");
		final var smErrandId = UUID.randomUUID().toString();
		final var fileName = "decision.pdf";
		final var extraParameters = List.of(new ExtraParameter().key(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).values(List.of("PASSENGER")));
		final var templateIdentifier = "sbk.rph.decision.passenger.rejection";

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(textProviderMock.getDenialTexts(MUNICIPALITY_ID)).thenReturn(denialTextPropertiesMock);
		when(denialTextPropertiesMock.getFilename()).thenReturn(fileName);
		when(commonTextPropertiesMock.getSendDigitalMail()).thenReturn(true);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(REJECTION)));
		when(errandMock.getStakeholders()).thenReturn(createApplicantAndAdministratorStakeholder());
		when(errandMock.getErrandNumber()).thenReturn(ERRAND_NUMBER);
		when(errandMock.getMunicipalityId()).thenReturn(MUNICIPALITY_ID);
		when(errandMock.getExtraParameters()).thenReturn(extraParameters);
		when(messagingServiceMock.sendDecisionMessage(MUNICIPALITY_ID, errandMock, pdf, false)).thenThrow(thrownException);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier)).thenReturn(pdf);
		when(supportManagementServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), any())).thenReturn(Optional.of(smErrandId));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(any(ExternalTask.class));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(supportManagementServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), any(generated.se.sundsvall.supportmanagement.Errand.class));
		verify(supportManagementServiceMock).createAttachment(MUNICIPALITY_ID, SM_NAMESPACE_CONTACTANGE, smErrandId, fileName, pdf.getOutput());
		verifyNoMoreInteractions(camundaClientMock, messagingServiceMock);
		verifyNoInteractions(failureHandlerMock);
	}

	@Test
	void executeThrowsException() {

		// Arrange
		final var thrownException = new EngineException("TestException", new RestException("message", "type", 1));
		final var pdf = new RenderResponse();
		final var messageUUID = UUID.randomUUID();
		final var smErrandId = UUID.randomUUID().toString();
		final var extraParameters = List.of(new ExtraParameter().key(CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY).values(List.of("PASSENGER")));
		final var templateIdentifier = "sbk.rph.decision.passenger.approval";

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(errandMock.getDecisions()).thenReturn(List.of(createFinalDecision(APPROVAL)));
		when(errandMock.getStakeholders()).thenReturn(createApplicantAndAdministratorStakeholder());
		when(errandMock.getExtraParameters()).thenReturn(extraParameters);
		when(textProviderMock.getCommonTexts(MUNICIPALITY_ID)).thenReturn(commonTextPropertiesMock);
		when(commonTextPropertiesMock.getSendDigitalMail()).thenReturn(true);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier)).thenReturn(pdf);
		when(messagingServiceMock.sendDecisionMessage(MUNICIPALITY_ID, errandMock, pdf, true)).thenReturn(messageUUID);
		when(supportManagementServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(SM_NAMESPACE_CONTACTANGE), any())).thenReturn(Optional.of(smErrandId));

		doThrow(thrownException).when(externalTaskServiceMock).complete(any(), anyMap());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(externalTaskServiceMock).complete(externalTaskMock, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageUUID.toString()));
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(messagingServiceMock).renderPdfDecision(MUNICIPALITY_ID, errandMock, templateIdentifier);
		verify(messagingServiceMock).sendDecisionMessage(MUNICIPALITY_ID, errandMock, pdf, true);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, thrownException.getMessage());
		verifyNoMoreInteractions(camundaClientMock, messagingServiceMock);
	}

	private Decision createFinalDecision(DecisionOutcomeEnum decisionOutcome) {
		final var decision = new Decision();
		decision.setId(1L);
		decision.setDecisionType(FINAL);
		decision.setDecisionOutcome(decisionOutcome);
		return decision;
	}

	private List<Stakeholder> createApplicantAndAdministratorStakeholder() {
		return List.of(new Stakeholder()
			.personId("personId")
			.firstName("Kalle")
			.lastName("Anka")
			.addresses(List.of(new Address().street("Storgatan").houseNumber("1").postalCode("12345").city("Ankeborg").careOf("c/o Anka")
				.country("Sverige")))
			.contactInformation(List.of(new ContactInformation().value("kalle.anka@ange.se").contactType(ContactInformation.ContactTypeEnum.EMAIL),
				new ContactInformation().value("0701234567").contactType(ContactInformation.ContactTypeEnum.PHONE)))
			.type(PERSON)
			.roles(List.of(ROLE_APPLICANT)),
			new Stakeholder()
				.personId("personId")
				.firstName("Kajsa")
				.lastName("Anka")
				.addresses(List.of(new Address().street("Storgatan").houseNumber("1").postalCode("12345").city("Ankeborg").careOf("c/o Anka")
					.country("Sverige")))
				.contactInformation(List.of(new ContactInformation().value("kalle.anka@ange.se").contactType(ContactInformation.ContactTypeEnum.EMAIL),
					new ContactInformation().value("0701234567").contactType(ContactInformation.ContactTypeEnum.PHONE)))
				.type(PERSON)
				.adAccount("adAccount")
				.roles(List.of(ROLE_ADMINISTRATOR)));
	}
}
