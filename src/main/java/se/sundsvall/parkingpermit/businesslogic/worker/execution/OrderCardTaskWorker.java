package se.sundsvall.parkingpermit.businesslogic.worker.execution;

import static java.util.Objects.isNull;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_LOST_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT;
import static se.sundsvall.parkingpermit.Constants.CASE_TYPE_PARKING_PERMIT_RENEWAL;
import static se.sundsvall.parkingpermit.integration.casedata.mapper.CaseDataMapper.toStatus;

import generated.se.sundsvall.casedata.Errand;
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

@Component
@ExternalTaskSubscription("OrderCardTask")
public class OrderCardTaskWorker extends AbstractTaskWorker {

	private static final String NO_CASE_TYPE = "Errand has no CaseType";
	private static final String UNSUPPORTED_CASE_TYPE = "CaseType '%s' is not supported";
	private static final String QUEUE_PARKING_PERMITS = "ParkingPermits";

	private final RpaService rpaService;

	OrderCardTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler, RpaService rpaService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.rpaService = rpaService;
	}

	@Override
	public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			rpaService.addQueueItems(getQueueNames(errand), errand.getId(), municipalityId);
			caseDataClient.patchStatus(externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID), namespace, errand.getId(), toStatus(CASEDATA_STATUS_DECISION_EXECUTED, CASEDATA_STATUS_DECISION_EXECUTED));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private List<String> getQueueNames(final Errand errand) {

		final var caseType = errand.getCaseType();

		if (isNull(caseType)) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, NO_CASE_TYPE);
		}

		if (CASE_TYPE_PARKING_PERMIT.equals(caseType) ||
			CASE_TYPE_PARKING_PERMIT_RENEWAL.equals(caseType) ||
			CASE_TYPE_LOST_PARKING_PERMIT.equals(caseType)) {
			return List.of(QUEUE_PARKING_PERMITS);
		}
		throw Problem.valueOf(INTERNAL_SERVER_ERROR, UNSUPPORTED_CASE_TYPE.formatted(errand.getCaseType()));
	}
}
