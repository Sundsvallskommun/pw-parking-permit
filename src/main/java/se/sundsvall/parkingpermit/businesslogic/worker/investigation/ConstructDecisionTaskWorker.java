package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import static generated.se.sundsvall.businessrules.ResultValue.NOT_APPLICABLE;
import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_DISABILITY_DURATION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDED;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

import generated.se.sundsvall.businessrules.RuleEngineResponse;
import generated.se.sundsvall.casedata.Decision;
import java.time.OffsetDateTime;
import java.time.Period;
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

	private static final Period VALIDITY_PERIOD_ONE_YEAR = Period.parse("P1Y");

	ConstructDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for ConstructDecisionTaskWorker");
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var latestDecision = errand.getDecisions().stream()
				.max(Comparator.comparingInt(Decision::getVersion)).orElse(null);

			final RuleEngineResponse ruleEngineResponse = externalTask.getVariable(CAMUNDA_VARIABLE_RULE_ENGINE_RESPONSE);

			validateResponse(ruleEngineResponse);

			final var isAutomatic = isAutomatic(errand);

			final var decision = ruleEngineResponse.getResults().stream()
				.filter(result -> !NOT_APPLICABLE.equals(result.getValue()))
				.findFirst()
				.map(result -> BusinessRulesUtil.constructDecision(result, isAutomatic))
				.orElseThrow(() -> Problem.valueOf(CONFLICT, "No applicable result found in rule engine response"));

			if (isAutomatic && APPROVAL.equals(decision.getDecisionOutcome())) {
				decision.setValidFrom(OffsetDateTime.now());

				final var disabilityDuration = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
					.filter(extraParameters -> CASEDATA_KEY_DISABILITY_DURATION.equals(extraParameters.getKey()))
					.findFirst()
					.flatMap(extraParameters -> extraParameters.getValues().stream().findFirst())
					.map(Period::parse)
					.orElseThrow(() -> Problem.valueOf(CONFLICT, "No disability duration found in errand"));

				decision.setValidTo(getValidTo(disabilityDuration));
			}

			if (isDecisionsNotEqual(latestDecision, decision)) {
				caseDataClient.patchNewDecision(
					municipalityId,
					errand.getNamespace(),
					caseNumber,
					decision.version(Optional.ofNullable(latestDecision).map(theDecision -> theDecision.getVersion() + 1).orElse(0)));
			}

			if (isAutomatic) {
				caseDataClient.patchStatus(errand.getMunicipalityId(), errand.getNamespace(), errand.getId(), toStatus(CASEDATA_STATUS_CASE_DECIDED, CASEDATA_STATUS_CASE_DECIDED));
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

	private OffsetDateTime getValidTo(Period validityPeriod) {
		if (!isValidDisabilityDuration(validityPeriod)) {
			throw Problem.valueOf(BAD_REQUEST, "No valid validity period found");
		}
		if (VALIDITY_PERIOD_ONE_YEAR.equals(validityPeriod)) {
			return OffsetDateTime.now().plus(Period.ofYears(1));
		}
		return OffsetDateTime.now().plus(Period.ofYears(2));
	}

	private boolean isValidDisabilityDuration(Period disabilityDuration) {
		if (isNull(disabilityDuration)) {
			return false;
		}
		// A disability duration cannot be shorter than 1 year if automatic decision. If it's zero means that the
		// disability is permanent.
		return VALIDITY_PERIOD_ONE_YEAR.getYears() <= disabilityDuration.getYears() || disabilityDuration.isZero();
	}
}
