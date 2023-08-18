package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.FALSE;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import generated.se.sundsvall.casedata.ErrandDTO;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

public abstract class AbstractTaskWorker implements ExternalTaskHandler {

	private Logger logger;

	@Autowired
	private CamundaClient camundaClient;

	@Autowired
	protected CaseDataClient caseDataClient;

	@Autowired
	protected FailureHandler failureHandler;

	protected AbstractTaskWorker() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	protected void clearUpdateAvailable(ExternalTask externalTask) {
		/* Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
	}

	protected ErrandDTO getErrand(ExternalTask externalTask) {
		return caseDataClient.getErrandById(externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER));
	}

	protected void logInfo(String msg, Object... arguments) {
		logger.info(msg, arguments);
	}

	protected void logException(ExternalTask externalTask, Exception exception) {
		logger.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);
	}

	protected abstract void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService);

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		RequestId.init(externalTask.getVariable(CAMUNDA_VARIABLE_REQUEST_ID));
		executeBusinessLogic(externalTask, externalTaskService);
	}
}
