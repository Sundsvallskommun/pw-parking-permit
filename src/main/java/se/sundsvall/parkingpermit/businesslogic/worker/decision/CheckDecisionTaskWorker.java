package se.sundsvall.parkingpermit.businesslogic.worker.decision;

import generated.se.sundsvall.casedata.DecisionDTO;
import generated.se.sundsvall.casedata.ErrandDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.HashMap;

import static generated.se.sundsvall.casedata.DecisionDTO.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.DecisionDTO.DecisionTypeEnum.FINAL;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractTaskWorker {

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			clearUpdateAvailable(externalTask);

			final var errand = getErrand(externalTask);

			final var variables = new HashMap<String, Object>();

			errand.getDecisions().stream()
				.filter(decision -> FINAL.equals(decision.getDecisionType()))
				.findFirst()
				.ifPresentOrElse(decision -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
					variables.put(CAMUNDA_VARIABLE_IS_APPROVED, isApproved(decision.getDecisionOutcome()));
					logInfo("Decision is made.");
				}, () -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
					logInfo("Decision is not made yet.");
				});
			if (isCancel(errand)) {
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
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
}
