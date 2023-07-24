package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toMessageAttachment;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toMessageRequest;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.DenialMessageProperties;

@Component
@ExternalTaskSubscription("AddMessageToErrandTask")
public class AddMessageToErrandTaskWorker extends AbstractWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(AddMessageToErrandTaskWorker.class);

	@Autowired
	private MessagingService messagingService;

	@Autowired
	DenialMessageProperties denialMessageProperties;

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final var errand = getErrand(externalTask);
			LOGGER.info("Executing addition of decision message to errand with id {}", errand.getId());

			final var pdf = messagingService.renderPdf(errand);
			final var attachment = toMessageAttachment(denialMessageProperties.filename(), APPLICATION_PDF_VALUE, pdf);
			final var messageId = ofNullable(externalTask.getVariable(CAMUNDA_VARIABLE_MESSAGE_ID))
				.map(String::valueOf)
				.orElseThrow(() -> Problem.valueOf(Status.INTERNAL_SERVER_ERROR, "Id of sent message could not be retreived from stored process variables"));

			caseDataClient.addMessage(toMessageRequest(messageId, denialMessageProperties.subject(), denialMessageProperties.plainBody(), errand, OUTBOUND, "ProcessEngine", attachment));

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			LOGGER.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
