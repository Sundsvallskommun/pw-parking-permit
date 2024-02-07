package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import java.util.HashMap;
import java.util.Optional;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractTaskWorker {

	CheckDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			clearUpdateAvailable(externalTask);

			final var errand = getErrand(externalTask);

			final var variables = new HashMap<String, Object>();

			Optional.ofNullable(errand.getStatuses()).orElse(emptyList()).stream()
				.filter(status -> CASEDATA_STATUS_CASE_DECIDED.equals(status.getStatusType()) || CASEDATA_STATUS_DECISION_EXECUTED.equals(status.getStatusType()))
				.findFirst()
				.ifPresentOrElse(status -> {
					if (isFinalDecision(errand)) {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
						logInfo("Decision is made.");
					} else {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
						variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
						caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
						logInfo("Decision is not made yet.");
					}
				}, () -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
					variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
					caseDataClient.patchErrand(errand.getId(), toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
					logInfo("Decision is not made yet.");
				});

			Optional.ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
				.filter(decision -> isApproved(decision.getDecisionOutcome()))
				.findFirst()
				.ifPresentOrElse(decision -> variables.put(CAMUNDA_VARIABLE_IS_APPROVED, true),
					() -> variables.put(CAMUNDA_VARIABLE_IS_APPROVED, false));

			if (isCancel(errand)) {
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isCancel(ErrandDTO errand) {
		return ofNullable(errand.getExtraParameters())
			.map(extraParameters -> extraParameters.get(CASEDATA_KEY_PHASE_ACTION))
			.filter(phaseAction -> phaseAction.equals(PHASE_ACTION_CANCEL))
			.isPresent();
	}

	private boolean isApproved(DecisionDTO.DecisionOutcomeEnum decisionOutcome) {
		return APPROVAL.equals(decisionOutcome);
	}

	private boolean isFinalDecision(ErrandDTO errand) {
		if (errand.getDecisions() == null) {
			return false;
		}
		return errand.getDecisions().stream()
			.anyMatch(decision -> FINAL.equals(decision.getDecisionType()));
	}
}
