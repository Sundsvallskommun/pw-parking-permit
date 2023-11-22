package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.businessrules.RuleEngineResponse;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.businesslogic.util.BusinessRulesUtil;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import static generated.se.sundsvall.businessrules.ResultValue.NOT_APPLICABLE;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE;

@Component
@ExternalTaskSubscription("InvestigationConstructDecisionTask")
public class ConstructDecisionTaskWorker extends AbstractTaskWorker {

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ConstructDecisionTaskWorker");

			final RuleEngineResponse ruleEngineResponse = externalTask.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE);

			validateResponse(ruleEngineResponse);

			final var decisionDTO = ruleEngineResponse.getResults().stream()
				.filter(result -> ! NOT_APPLICABLE.equals(result.getValue()))
				.findFirst()
				.map(BusinessRulesUtil::constructDecision)
				.orElseThrow(() -> Problem.valueOf(CONFLICT, "No applicable result found in rule engine response"));

			caseDataClient.patchNewDecision(externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER), decisionDTO);

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private void validateResponse(RuleEngineResponse ruleEngineResponse) {
		if (isNull(ruleEngineResponse)) {
			throw Problem.valueOf(BAD_REQUEST, "No rule engine response found");
		}

		if (isEmpty(ruleEngineResponse.getResults())) {
			throw Problem.valueOf(BAD_REQUEST, "No results found in rule engine response");
		}
	}
}
