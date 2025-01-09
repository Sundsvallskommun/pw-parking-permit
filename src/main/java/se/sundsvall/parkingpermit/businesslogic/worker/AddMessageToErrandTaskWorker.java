package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toMessageAttachment;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toMessageRequest;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.MessagingService;
import se.sundsvall.parkingpermit.util.TextProvider;

@Component
@ExternalTaskSubscription("AddMessageToErrandTask")
public class AddMessageToErrandTaskWorker extends AbstractTaskWorker {

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	AddMessageToErrandTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, MessagingService messagingService, TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
		this.textProvider = textProvider;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final String namespace = externalTask.getVariable(CAMUNDA_VARIABLE_NAMESPACE);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing addition of decision message to errand with id {}", errand.getId());

			final var pdf = messagingService.renderPdf(municipalityId, errand);
			final var attachment = toMessageAttachment(textProvider.getDenialTexts().filename(), APPLICATION_PDF_VALUE, pdf);
			final var messageId = ofNullable(externalTask.getVariable(CAMUNDA_VARIABLE_MESSAGE_ID))
				.map(String::valueOf)
				.orElseThrow(() -> Problem.valueOf(Status.INTERNAL_SERVER_ERROR, "Id of sent message could not be retreived from stored process variables"));

			caseDataClient.addMessage(municipalityId, namespace, caseNumber, toMessageRequest(messageId, textProvider.getDenialTexts().subject(), textProvider.getDenialTexts().plainBody(), errand, OUTBOUND, "ProcessEngine", attachment));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
