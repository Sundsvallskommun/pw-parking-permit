package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;
import static se.sundsvall.parkingpermit.integration.businessrules.mapper.BusinessRulesMapper.toRuleEngineRequest;

import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.businessrules.BusinessRulesClient;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("InvestigationExecuteRulesTask")
public class ExecuteRulesTaskWorker extends AbstractTaskWorker {

	private final BusinessRulesClient businessRulesClient;

	ExecuteRulesTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, BusinessRulesClient businessRulesClient) {
		super(camundaClient, caseDataClient, failureHandler);
		this.businessRulesClient = businessRulesClient;
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ExecuteRulesTaskWorker");
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber);

			final var ruleEngineResponse = businessRulesClient.runRuleEngine(municipalityId, toRuleEngineRequest(errand));

			final var variables = new HashMap<String, Object>();
			variables.put(Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
