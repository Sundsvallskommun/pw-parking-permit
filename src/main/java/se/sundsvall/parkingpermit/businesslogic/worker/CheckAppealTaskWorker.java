package se.sundsvall.parkingpermit.businesslogic.worker;

import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_IS_IN_TIMELINESS_REVIEW;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_PARKING_PERMIT_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_APPEAL;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.RelatedErrand;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("CheckAppealTask")
public class CheckAppealTaskWorker extends AbstractTaskWorker {

	private static final int DAYS_IN_TIMELINESS_REVIEW = 21;
	private static final String APPEALED_ERRAND_IS_MISSING_FINAL_DECISION = "Appealed errand with id '%s' in namespace:'%s' for municipality with id:'%s' is missing final decision";
	private static final String DECIDED_AT_IS_MISSING_IN_APPEALED_DECISION = "Decided at is missing in appealed decision with id '%s' in namespace:'%s' for municipality with id:'%s'";
	private static final String APPEAL_IS_MISSING_RELATED_ERRAND = "Appeal with id '%s' in namespace:'%s' for municipality with id:'%s' has no related errand";

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

			var isInTimelinessReview = false;

			if (isAppeal) {
				final var relatedErrand = getAppealedErrand(errand);

				final var appealedErrand = getErrand(municipalityId, CASEDATA_PARKING_PERMIT_NAMESPACE, relatedErrand.getErrandId());

				isInTimelinessReview = isInTimelinessReview(errand.getApplicationReceived(), appealedErrand);
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
		final var appealedDecision = appealedErrand.getDecisions().stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, APPEALED_ERRAND_IS_MISSING_FINAL_DECISION.formatted(appealedErrand.getId(), appealedErrand.getNamespace(), appealedErrand.getMunicipalityId())));

		return ofNullable(appealedDecision.getDecidedAt()).orElseThrow(() -> Problem.valueOf(Status.BAD_REQUEST, DECIDED_AT_IS_MISSING_IN_APPEALED_DECISION.formatted(appealedErrand.getId(), appealedErrand.getNamespace(), appealedErrand
			.getMunicipalityId())))
			.plusDays(DAYS_IN_TIMELINESS_REVIEW).isAfter(applicationReceived);
	}

	private RelatedErrand getAppealedErrand(Errand errand) {
		return ofNullable(errand.getRelatesTo()).orElse(Collections.emptyList()).stream()
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, APPEAL_IS_MISSING_RELATED_ERRAND.formatted(errand.getId(), errand.getNamespace(), errand.getMunicipalityId())));
	}
}
