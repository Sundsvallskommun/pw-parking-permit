package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.TEMPLATE_IDENTIFIER;

import java.util.Map;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;

@Component
@ExternalTaskSubscription("SendDenialDecisionTask")
public class SendDenialDecisionTaskWorker extends AbstractTaskWorker {

	private final MessagingService messagingService;

	SendDenialDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, MessagingService messagingService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing delivery of decision message to applicant for errand with id {}", errand.getId());

			// TODO: Get template identifier from configuration
			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, TEMPLATE_IDENTIFIER);
			final var messageId = messagingService.sendMessageToNonCitizen(municipalityId, errand, pdf).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
