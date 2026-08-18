package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.casedata.Stakeholder;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.TextProvider;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.parkingpermit.Constants.CATEGORY_BESLUT;
import static se.sundsvall.parkingpermit.Constants.LAW_ARTICLE;
import static se.sundsvall.parkingpermit.Constants.LAW_CHAPTER;
import static se.sundsvall.parkingpermit.Constants.LAW_HEADING;
import static se.sundsvall.parkingpermit.Constants.LAW_SFS;
import static se.sundsvall.parkingpermit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toAttachment;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toAttachmentFilePart;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toAttachmentMetadataPart;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toDecision;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toLaw;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStakeholder;
import static se.sundsvall.parkingpermit.util.LocationUtil.extractIdFromLocation;
import static se.sundsvall.parkingpermit.util.TimerUtil.getControlMessageTime;

@Component
@ExternalTaskSubscription("AutomaticDenialDecisionTask")
public class AutomaticDenialDecisionTaskWorker extends AbstractTaskWorker {

	private static final String PROCESS_ENGINE_FIRST_NAME = "Process";
	private static final String PROCESS_ENGINE_LAST_NAME = "Engine";

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	AutomaticDenialDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, MessagingService messagingService, TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
		this.textProvider = textProvider;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing automatic addition of dismissal to errand with id {}", errand.getId());

			// PE needs to be added as stakeholder to the errand (if not already present) and store for later use when setting
			// "decidedBy" on decision
			final var stakeholder = ofNullable(errand.getStakeholders()).orElse(emptyList())
				.stream()
				.filter(AutomaticDenialDecisionTaskWorker::isProcessEngineStakeholder)
				.findAny()
				.orElseGet(() -> createProcessEngineStakeholder(errand.getId(), municipalityId, namespace));

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, textProvider.getDenialTexts(municipalityId).getTemplateId());
			final var decision = toDecision(FINAL, DISMISSAL, textProvider.getDenialTexts(municipalityId).getDescription())
				.decidedBy(stakeholder)
				.decidedAt(OffsetDateTime.now(ZoneId.systemDefault()))
				.addLawItem(toLaw(LAW_HEADING, LAW_SFS, LAW_CHAPTER, LAW_ARTICLE));

			// The decision has to exist before its attachment can be uploaded, since CaseData rejects attachments sent as part
			// of the decision payload
			final var decisionId = extractIdFromLocation(caseDataClient.patchNewDecision(municipalityId, namespace, errand.getId(), decision), "decision");

			final var filename = textProvider.getCommonTexts(municipalityId).getFilename();
			caseDataClient.postDecisionAttachment(municipalityId, namespace, errand.getId(), decisionId,
				toAttachmentMetadataPart(toAttachment(CATEGORY_BESLUT, filename, "pdf", APPLICATION_PDF_VALUE)),
				toAttachmentFilePart(filename, APPLICATION_PDF_VALUE, pdf));

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE, getControlMessageTime(decision, textProvider.getSimplifiedServiceTexts(municipalityId).getDelay()));

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private Stakeholder createProcessEngineStakeholder(final Long errandId, final String municipalityId, final String namespace) {
		final var id = extractIdFromLocation(caseDataClient.addStakeholderToErrand(municipalityId, namespace, errandId, toStakeholder(ROLE_ADMINISTRATOR, PERSON, PROCESS_ENGINE_FIRST_NAME, PROCESS_ENGINE_LAST_NAME)), "stakeholder");
		return caseDataClient.getStakeholder(municipalityId, namespace, errandId, id);
	}

	private static boolean isProcessEngineStakeholder(Stakeholder stakeholder) {
		return stakeholder.getRoles().contains(ROLE_ADMINISTRATOR) &&
			PROCESS_ENGINE_FIRST_NAME.equals(stakeholder.getFirstName()) &&
			PROCESS_ENGINE_LAST_NAME.equals(stakeholder.getLastName());
	}
}
