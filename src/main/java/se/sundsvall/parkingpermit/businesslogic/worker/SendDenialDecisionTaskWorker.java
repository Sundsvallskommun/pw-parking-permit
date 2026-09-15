package se.sundsvall.parkingpermit.businesslogic.worker;

import java.util.Map;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.integration.engine.EngineClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.TextProvider;

import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_MESSAGE_ID;

@Component
@ExternalTaskSubscription("SendDenialDecisionTask")
public class SendDenialDecisionTaskWorker extends AbstractTaskWorker {

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	SendDenialDecisionTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler, final MessagingService messagingService,
		TextProvider textProvider) {
		super(engineClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
		this.textProvider = textProvider;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing delivery of decision message to applicant for errand with id {}", errand.getId());

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, textProvider.getDenialTexts(municipalityId).getTemplateId());
			final var messageId = messagingService.sendMessageToNonCitizen(municipalityId, errand, pdf).toString();

			externalTaskService.complete(externalTask, Map.of(PROCESS_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
