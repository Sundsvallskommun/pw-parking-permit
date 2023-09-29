package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import generated.se.sundsvall.casedata.ErrandDTO;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;
import se.sundsvall.parkingpermit.service.RpaService;

import java.util.List;

import static java.util.Objects.isNull;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

@Component
@ExternalTaskSubscription("OrderCardTask")
public class OrderCardTaskWorker extends AbstractTaskWorker {

	private static final String NO_CASE_TYPE = "Errand has no CaseType";
	private static final String UNSUPPORTED_CASE_TYPE = "CaseType '%s' is not supported";
	private static final String QUEUE_NEW_CARD = "NyttKortNyPerson";
	private static final String QUEUE_REPLACEMENT_CARD = "NyttKortBefintligPerson";
	private static final String QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD = "StöldspärraSamtSkapaNyttKort";

	@Autowired
	private RpaService rpaService;


	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			ErrandDTO errand = getErrand(externalTask);
			rpaService.addQueueItems(getQueueNames(errand), errand.getId());
			caseDataClient.putStatus(errand.getId(), List.of(toStatus(CASEDATA_STATUS_DECISION_EXECUTED, CASEDATA_STATUS_DECISION_EXECUTED)));

			externalTaskService.complete(externalTask);
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
    }

	private List<String> getQueueNames(ErrandDTO errand) {
		if (isNull(errand.getCaseType())) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, NO_CASE_TYPE);
		}

		return switch (errand.getCaseType()) {
			case PARKING_PERMIT -> List.of(QUEUE_NEW_CARD);
			case PARKING_PERMIT_RENEWAL -> List.of(QUEUE_REPLACEMENT_CARD);
			case LOST_PARKING_PERMIT -> List.of(QUEUE_ANTI_THEFT_AND_REPLACEMENT_CARD);

			default -> throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format(UNSUPPORTED_CASE_TYPE, errand.getCaseType().name()));
		};
	}
}
