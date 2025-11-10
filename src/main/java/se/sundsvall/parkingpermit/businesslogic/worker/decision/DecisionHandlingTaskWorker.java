package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.REJECTION;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.CAPACITY_DRIVER;
import static se.sundsvall.parkingpermit.Constants.CAPACITY_PASSENGER;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY;
import static se.sundsvall.parkingpermit.Constants.SM_NAMESPACE_CONTACTANGE;
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

	DecisionHandlingTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler,
		final TextProvider textProvider, final MessagingService messagingService, final SupportManagementService supportManagementService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.textProvider = textProvider;
		this.messagingService = messagingService;
		this.supportManagementService = supportManagementService;
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for DecisionHandlingTask");
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, getTemplateId(errand));
			final boolean sendDigitalMail = textProvider.getCommonTexts(municipalityId).getSendDigitalMail();
			String messageId = null;

			if (isNotEmpty(errand.getExternalCaseId())) {
				// If the errand has an externalCaseId we will try to send a web message
				messageId = Optional.ofNullable(messagingService.sendDecisionWebMessage(municipalityId, errand, pdf, getFinalDecision(errand)))
					.map(UUID::toString)
					.orElse(null);
			}
			if (sendDigitalMail && isNull(messageId)) {
				// Try to send digital mail if configured to do so and if the errand does not have an externalCaseId
				messageId = sendDigitalMail(errand, municipalityId, pdf);
			}
			// If messageId is null here we have failed to send both web message and digital mail, and will create a support
			// management errand instead
			if (isNull(messageId)) {
				createSupportManagementMailingErrand(errand, municipalityId, SM_NAMESPACE_CONTACTANGE, pdf);
				createSupportManagementCardErrand(errand, municipalityId, SM_NAMESPACE_CONTACTANGE);
			} else {
				createSupportManagementCardErrand(errand, municipalityId, SM_NAMESPACE_CONTACTANGE);
				externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
				return;
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private String sendDigitalMail(final Errand errand, final String municipalityId, final RenderResponse pdf) {
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

	private void createSupportManagementMailingErrand(final Errand errand, final String municipalityId, final String namespace, final RenderResponse pdf) {
		final var labels = supportManagementService.getMetadataLabels(municipalityId, namespace);
		final var mailingErrandId = supportManagementService.createErrand(municipalityId, namespace, toSupportManagementMailingErrand(errand, isAutomatic(errand), labels));
		mailingErrandId.ifPresent(errandId -> supportManagementService.createAttachment(municipalityId, namespace, errandId, getFilename(errand), pdf.getOutput()));
	}

	private void createSupportManagementCardErrand(final Errand errand, final String municipalityId, final String namespace) {
		if (isApproved(errand)) {
			final var labels = supportManagementService.getMetadataLabels(municipalityId, namespace);
			supportManagementService.createErrand(municipalityId, namespace, toSupportManagementCardManagementErrand(errand, isAutomatic(errand), labels));
		}
	}

	private String getFilename(final Errand errand) {
		return textProvider.getCommonTexts(errand.getMunicipalityId()).getFilename();
	}

	private String getTemplateId(final Errand errand) {
		StringBuilder templateId = new StringBuilder("sbk.rph.decision");
		final var capacity = Optional.ofNullable(errand.getExtraParameters()).orElse(emptyList())
			.stream()
			.filter(param -> CASEDATA_KEY_APPLICATION_APPLICANT_CAPACITY.equals(param.getKey()))
			.findFirst()
			.map(generated.se.sundsvall.casedata.ExtraParameter::getValues)
			.flatMap(values -> values.stream().findFirst())
			.orElse(null);

		if (CAPACITY_PASSENGER.equalsIgnoreCase(capacity)) {
			templateId.append(".passenger");
		} else if (CAPACITY_DRIVER.equalsIgnoreCase(capacity)) {
			templateId.append(".driver");
		} else {
			templateId.append(".all");
		}

		if (isApproved(errand)) {
			templateId.append(".approval");
		} else {
			templateId.append(".rejection");
		}

		if (isAutomatic(errand)) {
			templateId.append(".automatic");
		}

		return templateId.toString();
	}

	private boolean isApproved(final Errand errand) {
		final var decisionOutCome = Optional.ofNullable(getFinalDecision(errand))
			.map(Decision::getDecisionOutcome)
			.orElse(REJECTION);
		return APPROVAL.equals(decisionOutCome);
	}
}
