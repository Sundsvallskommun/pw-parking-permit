package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

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
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			final var attachments = getErrandAttachments(municipalityId, namespace, caseNumber);

			final var ruleEngineResponse = businessRulesClient.runRuleEngine(municipalityId, toRuleEngineRequest(errand, attachments));

			final var variables = new HashMap<String, Object>();
			variables.put(Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE, ruleEngineResponse);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
