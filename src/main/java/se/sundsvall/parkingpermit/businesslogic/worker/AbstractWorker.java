package se.sundsvall.parkingpermit.businesslogic.worker;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.FALSE;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.springframework.beans.factory.annotation.Autowired;

import generated.se.sundsvall.casedata.ErrandDTO;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

public abstract class AbstractWorker implements ExternalTaskHandler {

	@Autowired
	private CamundaClient camundaClient;

	@Autowired
	protected CaseDataClient caseDataClient;

	@Autowired
	protected FailureHandler failureHandler;

	protected void clearUpdateAvailable(ExternalTask externalTask) {
		/* Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
	}

	protected ErrandDTO getErrand(ExternalTask externalTask) {
		return caseDataClient.getErrandById(externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER));
	}
}
