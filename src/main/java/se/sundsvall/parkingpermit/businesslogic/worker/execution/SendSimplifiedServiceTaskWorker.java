package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;

import java.util.Map;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
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
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber);
			logInfo("Executing delivery of simplified service message to applicant for errand with id {}", errand.getId());

			final var messageId = messagingService.sendMessageSimplifiedService(municipalityId, errand).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}