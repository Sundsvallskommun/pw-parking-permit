package se.sundsvall.parkingpermit.businesslogic.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.util.TextProvider;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toMessageRequest;

@Component
@ExternalTaskSubscription("AddSimplifiedServiceMessageToErrandTask")
public class AddSimplifiedServiceMessageToErrandTaskWorker extends AbstractTaskWorker {

	private final TextProvider textProvider;

	AddSimplifiedServiceMessageToErrandTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.textProvider = textProvider;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String messageId = externalTask.getVariable(CAMUNDA_VARIABLE_SIMPLIFIED_SERVICE_MESSAGE_ID);

			if (messageId == null) {
				externalTaskService.complete(externalTask);
				return;
			}

			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing addition of simplified service message to errand with id {}", errand.getId());

			final var texts = textProvider.getSimplifiedServiceTexts(municipalityId);
			caseDataClient.addMessage(municipalityId, namespace, caseNumber,
				toMessageRequest(messageId, texts.getSubject(), texts.getPlainBody(), errand, OUTBOUND, "ProcessEngine", null));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
