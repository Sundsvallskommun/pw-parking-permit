package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static generated.se.sundsvall.businessrules.ResultValue.NOT_APPLICABLE;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE;

import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.Decision;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.util.BusinessRulesUtil;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("InvestigationConstructDecisionTask")
public class ConstructDecisionTaskWorker extends AbstractTaskWorker {

	ConstructDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ConstructDecisionTaskWorker");
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final String namespace = externalTask.getVariable(CAMUNDA_VARIABLE_NAMESPACE);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var latestDecision = errand.getDecisions().stream()
				.max(Comparator.comparingInt(Decision::getVersion)).orElse(null);

			final RuleEngineResponse ruleEngineResponse = externalTask.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE);

			validateResponse(ruleEngineResponse);

			final var decision = ruleEngineResponse.getResults().stream()
				.filter(result -> !NOT_APPLICABLE.equals(result.getValue()))
				.findFirst()
				.map(BusinessRulesUtil::constructDecision)
				.orElseThrow(() -> Problem.valueOf(CONFLICT, "No applicable result found in rule engine response"));

			if (isDecisionsNotEqual(latestDecision, decision)) {
				caseDataClient.patchNewDecision(
					municipalityId,
					errand.getNamespace(),
					caseNumber,
					decision.version(Optional.ofNullable(latestDecision).map(theDecision -> theDecision.getVersion() + 1).orElse(0)));
			}

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
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

	private boolean isDecisionsNotEqual(Decision latestDecision, Decision decision) {
		if (isNull(latestDecision)) {
			return true;
		}
		return !(Objects.equals(latestDecision.getDecisionType(), decision.getDecisionType())
			&& Objects.equals(latestDecision.getDecisionOutcome(), decision.getDecisionOutcome())
			&& Objects.equals(latestDecision.getDescription(), decision.getDescription()));
	}
}
