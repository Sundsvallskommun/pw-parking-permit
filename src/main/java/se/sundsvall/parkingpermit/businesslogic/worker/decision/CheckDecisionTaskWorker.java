package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.HashMap;
import java.util.Map;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			final var errand = getErrand(externalTask);

			final var variables = new HashMap<String, Object>(Map.of(CAMUNDA_VARIABLE_UPDATE_AVAILABLE, false));

			errand.getDecisions().stream()
				.filter(decision -> FINAL.equals(decision.getDecisionType()))
				.findFirst()
				.ifPresentOrElse(decision -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
					logInfo("Decision is made.");
				}, () -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
					logInfo("Decision is not made yet.");
				});

			externalTaskService.complete(externalTask, variables);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
