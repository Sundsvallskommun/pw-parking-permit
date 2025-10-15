package se.sundsvall.parkingpermit.businesslogic.worker;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.MUNICIPALITY_ID_ANGE;

import generated.se.sundsvall.casedata.Errand;
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
@ExternalTaskSubscription("SendSimplifiedServiceTask")
public class SendSimplifiedServiceTaskWorker extends AbstractTaskWorker {

	private final MessagingService messagingService;

	SendSimplifiedServiceTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, MessagingService messagingService) {
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
			logInfo("Executing delivery of simplified service message to applicant for errand with id {}", errand.getId());

			if (shouldNotSendMessage(municipalityId, errand)) {
				externalTaskService.complete(externalTask);
				return;
			}

			final var messageId = messagingService.sendMessageSimplifiedService(municipalityId, errand).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean shouldNotSendMessage(String municipalityId, Errand errand) {
		return MUNICIPALITY_ID_ANGE.equals(municipalityId) && isBlank(errand.getExternalCaseId());
	}
}
