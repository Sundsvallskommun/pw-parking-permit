package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.ErrandDTO;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CARD_EXISTS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER;

@Component
@ExternalTaskSubscription("CardExistsTask")
public class CheckCardExistsTaskWorker extends AbstractTaskWorker {
	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CardExistsTask");
			clearUpdateAvailable(externalTask);
			final var errand = getErrand(externalTask);

			final var cardExists = isCardCreated(errand);

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_CARD_EXISTS, cardExists));
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isCardCreated(ErrandDTO errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyMap()).entrySet().stream()
				.filter(entry -> CASEDATA_KEY_ARTEFACT_PERMIT_NUMBER.equals(entry.getKey()))
				.map(Map.Entry::getValue)
				.anyMatch(StringUtils::isNotEmpty);
	}
}