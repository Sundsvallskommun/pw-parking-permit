package se.sundsvall.parkingpermit.businesslogic.worker;

import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.parkingpermit.Constants.FALSE;
import static se.sundsvall.parkingpermit.Constants.PHASE_ACTION_CANCEL;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.ErrandDTO;

public abstract class AbstractTaskWorker implements ExternalTaskHandler {

	private final Logger logger;

	private final CamundaClient camundaClient;

	protected final CaseDataClient caseDataClient;

	protected final FailureHandler failureHandler;

	protected AbstractTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.camundaClient = camundaClient;
		this.caseDataClient = caseDataClient;
		this.failureHandler = failureHandler;
	}

	protected void clearUpdateAvailable(ExternalTask externalTask) {
		/* Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
	}

	protected void setProcessInstanceVariable(ExternalTask externalTask, String variableName, VariableValueDto variableValue) {
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), variableName, variableValue);
	}

	protected ErrandDTO getErrand(String municipalityId, Long caseNumber) {
		return caseDataClient.getErrandById(municipalityId, caseNumber);
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

	protected boolean isCancel(ErrandDTO errand) {
		return ofNullable(errand.getExtraParameters())
				.map(extraParameters -> extraParameters.get(CASEDATA_KEY_PHASE_ACTION))
				.filter(PHASE_ACTION_CANCEL::equals)
				.isPresent();
	}
}
