package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.businessrules.BusinessRulesClient;

import java.util.HashMap;

import static se.sundsvall.parkingpermit.integration.businessrules.mapper.BusinessRulesMapper.toRuleEngineRequest;

@Component
@ExternalTaskSubscription("InvestigationExecuteRulesTask")
public class ExecuteRulesTaskWorker extends AbstractTaskWorker {

	@Autowired
	private BusinessRulesClient businessRulesClient;

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ExecuteRulesTaskWorker");
			final var errand = getErrand(externalTask);
			final var ruleEngineResponse = businessRulesClient.runRuleEngine(toRuleEngineRequest(errand));

			final var variables = new HashMap<String, Object>();
			variables.put(Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse);

			externalTaskService.complete(externalTask, variables);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
