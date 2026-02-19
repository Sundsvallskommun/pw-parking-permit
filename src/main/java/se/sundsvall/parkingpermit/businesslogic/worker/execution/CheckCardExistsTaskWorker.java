package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CARD_EXISTS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;

@Component
@ExternalTaskSubscription("CardExistsTask")
public class CheckCardExistsTaskWorker extends AbstractTaskWorker {

	CheckCardExistsTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CardExistsTask");
			clearUpdateAvailable(externalTask);
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var cardExists = isCardCreated(errand);

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_CARD_EXISTS, cardExists));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isCardCreated(Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameter -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(extraParameter.getKey()))
			.map(ExtraParameter::getValues)
			.map(List::getFirst)
			.anyMatch(StringUtils::isNotEmpty);
	}
}
