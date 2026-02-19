package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.util.TextProvider;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
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
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toExtraParameterList;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;
import static se.sundsvall.parkingpermit.util.TimerUtil.getControlMessageTime;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractTaskWorker {

	private final TextProvider textProvider;

	CheckDecisionTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.textProvider = textProvider;

	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			clearUpdateAvailable(externalTask);
			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var variables = new HashMap<String, Object>();

			if (isCancel(errand)) {
				logInfo("Errand is canceled.");
				// isFinalDecision and isApproved must be set because they are used in model
				variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
				variables.put(CAMUNDA_VARIABLE_IS_APPROVED, false);
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);
			} else {
				handleDecisionStatus(errand, municipalityId, namespace, variables);
				handleApprovalStatus(errand, variables);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private void handleDecisionStatus(Errand errand, String municipalityId, String namespace, Map<String, Object> variables) {
		boolean hasDecisionStatus = Optional.ofNullable(errand.getStatuses()).orElse(emptyList()).stream()
			.anyMatch(status -> CASEDATA_STATUS_CASE_DECIDED.equals(status.getStatusType())
				|| CASEDATA_STATUS_DECISION_EXECUTED.equals(status.getStatusType()));

		if (hasDecisionStatus && isFinalDecision(errand)) {
			variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
			logInfo("Decision is made.");
			variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE,
				getControlMessageTime(getFinalDecision(errand), textProvider.getSimplifiedServiceTexts(municipalityId).getDelay()));
		} else {
			variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
			variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
			patchErrandToWaiting(municipalityId, namespace, errand);
			logInfo("Decision is not made yet.");
		}
	}

	private void handleApprovalStatus(Errand errand, Map<String, Object> variables) {
		boolean isApproved = Optional.ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.anyMatch(decision -> isApproved(decision.getDecisionOutcome()));
		variables.put(CAMUNDA_VARIABLE_IS_APPROVED, isApproved);
	}

	private void patchErrandToWaiting(String municipalityId, String namespace, Errand errand) {
		caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(),
			toPatchErrand(errand.getExternalCaseId(), CASEDATA_PHASE_DECISION));
		caseDataClient.patchErrandExtraParameters(municipalityId, namespace, errand.getId(),
			toExtraParameterList(PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN, CASEDATA_PHASE_DECISION));
	}

	private boolean isApproved(Decision.DecisionOutcomeEnum decisionOutcome) {
		return APPROVAL.equals(decisionOutcome);
	}

	private boolean isFinalDecision(Errand errand) {
		return Optional.ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
			.anyMatch(decision -> FINAL.equals(decision.getDecisionType()));
	}
}
