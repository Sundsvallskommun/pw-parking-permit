package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Objects.isNull;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.TEMPLATE_IDENTIFIER;
import static se.sundsvall.parkingpermit.integration.supportmanagement.mapper.SupportManagementMapper.toSupportManagementCardManagementErrand;
import static se.sundsvall.parkingpermit.integration.supportmanagement.mapper.SupportManagementMapper.toSupportManagementMailingErrand;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.service.SupportManagementService;
import se.sundsvall.parkingpermit.util.TextProvider;

@Component
@ExternalTaskSubscription("DecisionHandlingTask")
public class DecisionHandlingTaskWorker extends AbstractTaskWorker {

	private final TextProvider textProvider;
	private final MessagingService messagingService;
	private final SupportManagementService supportManagementService;

	DecisionHandlingTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, TextProvider textProvider,
		MessagingService messagingService, SupportManagementService supportManagementService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.textProvider = textProvider;
		this.messagingService = messagingService;
		this.supportManagementService = supportManagementService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for DecisionHandlingAutomaticTask");
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			// TODO: Get template identifier from configuration
			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, TEMPLATE_IDENTIFIER);
			final boolean sendDigitalMail = textProvider.getCommonTexts(municipalityId).getSendDigitalMail();
			String messageId = null;

			if (sendDigitalMail) {
				messageId = sendDigitalMail(errand, municipalityId, pdf);
			}
			// If sending the decision message fails, or it's configured to not send digital mail, we will create support errands
			if (isNull(messageId)) {
				createSupportManagementErrands(errand, municipalityId, namespace, pdf);
			} else {
				externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
				return;
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private String sendDigitalMail(Errand errand, String municipalityId, RenderResponse pdf) {
		UUID messageId = null;
		try {
			messageId = messagingService.sendDecisionMessage(municipalityId, errand, pdf, isApproved(errand));
		} catch (final Exception e) {
			logInfo("Failed to send decision message");
		}
		return Optional.ofNullable(messageId)
			.map(UUID::toString)
			.orElse(null);
	}

	private void createSupportManagementErrands(Errand errand, String municipalityId, String namespace, RenderResponse pdf) {
		final var mailingErrandId = supportManagementService.createErrand(municipalityId, namespace, toSupportManagementMailingErrand(errand));
		mailingErrandId.ifPresent(errandId -> supportManagementService.createAttachment(municipalityId, namespace, errandId, getFilename(errand), pdf.getOutput()));

		if (isApproved(errand)) {
			supportManagementService.createErrand(municipalityId, namespace, toSupportManagementCardManagementErrand(errand));
		}
	}

	private boolean isApproved(Errand errand) {
		final var decisionOutCome = Optional.ofNullable(getFinalDecision(errand))
			.map(Decision::getDecisionOutcome)
			.orElse(REJECTION);
		return APPROVAL.equals(decisionOutCome);
	}

	private Decision getFinalDecision(Errand errand) {
		return errand.getDecisions().stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.orElse(null);
	}

	private String getFilename(Errand errand) {
		return isApproved(errand) ? textProvider.getApprovalTexts(errand.getMunicipalityId()).getFilename() : textProvider.getDenialTexts(errand.getMunicipalityId()).getFilename();
	}
}
