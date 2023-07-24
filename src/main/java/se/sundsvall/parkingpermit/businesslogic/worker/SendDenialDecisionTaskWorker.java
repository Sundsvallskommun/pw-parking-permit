package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;

import java.util.Map;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.sundsvall.parkingpermit.service.MessagingService;

@Component
@ExternalTaskSubscription("SendDenialDecisionTask")
public class SendDenialDecisionTaskWorker extends AbstractTaskWorker {

	@Autowired
	private MessagingService messagingService;

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			logInfo("Executing delivery of decision message to applicant for errand with id {}", errand.getId());

			final var pdf = messagingService.renderPdf(errand);
			final var messageId = messagingService.sendMessageToNonCitizen(errand, pdf).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
