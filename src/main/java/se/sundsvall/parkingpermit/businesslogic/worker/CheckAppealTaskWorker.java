package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_APPEAL;

import generated.se.sundsvall.casedata.Errand;
import java.time.OffsetDateTime;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("CheckAppealTask")
public class CheckAppealTaskWorker extends AbstractTaskWorker {

	private static final int DAYS_IN_TIMELINESS_REVIEW = 21;

	CheckAppealTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, caseNumber);
			logInfo("Check if errand is an appeal for errand with id {}", errand.getId());

			final var isAppeal = isAppeal(errand);

			var isInTimelinessReview = true;

			if (isAppeal) {
				// TODO Get appealed errand
				isInTimelinessReview = isInTimelinessReview(errand.getApplicationReceived(), null);
				logInfo("Errand with id {} is an appeal", errand.getId());
			}

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW, isInTimelinessReview);
			variables.put(CAMUNDA_VARIABLE_IS_APPEAL, isAppeal);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isAppeal(Errand errand) {
		return CASE_TYPE_APPEAL.equals(errand.getCaseType());
	}

	private boolean isInTimelinessReview(OffsetDateTime applicationReceived, Errand appealedErrand) {
		// TODO Implement logic for checking if errand is in timeliness review
		return true;
	}
}
