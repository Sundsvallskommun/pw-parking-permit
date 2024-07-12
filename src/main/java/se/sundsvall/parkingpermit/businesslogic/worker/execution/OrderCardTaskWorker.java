package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static java.util.Objects.isNull;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

import java.util.List;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;
import se.sundsvall.parkingpermit.service.RpaService;

import generated.se.sundsvall.casedata.ErrandDTO;

@Component
@ExternalTaskSubscription("OrderCardTask")
public class OrderCardTaskWorker extends AbstractTaskWorker {

	private static final String NO_CASE_TYPE = "Errand has no CaseType";
	private static final String UNSUPPORTED_CASE_TYPE = "CaseType '%s' is not supported";
	private static final String QUEUE_NEW_CARD = "NyttKortNyPerson";
	private static final String QUEUE_REPLACEMENT_CARD = "NyttKortBefintligPerson";
	private static final String QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD = "StöldspärraSamtSkapaNyttKort";

	private final RpaService rpaService;

	OrderCardTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, RpaService rpaService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.rpaService = rpaService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
			final Long caseNumber = externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);

			final var errand = getErrand(municipalityId, caseNumber);
			rpaService.addQueueItems(getQueueNames(errand), errand.getId());
			caseDataClient.putStatus(externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID), errand.getId(), List.of(toStatus(CASEDATA_STATUS_DECISION_EXECUTED, CASEDATA_STATUS_DECISION_EXECUTED)));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private List<String> getQueueNames(ErrandDTO errand) {
		if (isNull(errand.getCaseType())) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, NO_CASE_TYPE);
		}

		return switch (errand.getCaseType()) {
			case CASE_TYPE_PARKING_PERMIT -> List.of(QUEUE_NEW_CARD);
			case CASE_TYPE_PARKING_PERMIT_RENEWAL -> List.of(QUEUE_REPLACEMENT_CARD);
			case CASE_TYPE_LOST_PARKING_PERMIT -> List.of(QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD);
			default ->
				throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format(UNSUPPORTED_CASE_TYPE, errand.getCaseType()));
		};
	}
}
