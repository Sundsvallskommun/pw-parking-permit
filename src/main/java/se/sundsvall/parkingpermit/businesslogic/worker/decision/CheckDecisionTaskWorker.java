package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.util.SimplifiedServiceTextProperties;

import java.util.HashMap;
import java.util.Optional;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_CASE_DECIDED;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.parkingpermit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;
import static se.sundsvall.parkingpermit.util.TimerUtil.getControlMessageTime;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractTaskWorker {

	private final SimplifiedServiceTextProperties simplifiedServiceTextProperties;

	CheckDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, SimplifiedServiceTextProperties simplifiedServiceTextProperties) {
		super(camundaClient, caseDataClient, failureHandler);
		this.simplifiedServiceTextProperties = simplifiedServiceTextProperties;

	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			clearUpdateAvailable(externalTask);
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, caseNumber);

			final var variables = new HashMap<String, Object>();

			Optional.ofNullable(errand.getStatuses()).orElse(emptyList()).stream()
				.filter(status -> CASEDATA_STATUS_CASE_DECIDED.equals(status.getStatusType()) || CASEDATA_STATUS_DECISION_EXECUTED.equals(status.getStatusType()))
				.findFirst()
				.ifPresentOrElse(status -> {
					if (isFinalDecision(errand)) {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
						logInfo("Decision is made.");
						variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE, getControlMessageTime(simplifiedServiceTextProperties.delayDays()));
					} else {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
						variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
						caseDataClient.patchErrand(municipalityId, errand.getId(), toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN, CASEDATA_PHASE_DECISION));
						logInfo("Decision is not made yet.");
					}
				}, () -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
					variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
					caseDataClient.patchErrand(municipalityId, errand.getId(), toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN, CASEDATA_PHASE_DECISION));
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
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
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
