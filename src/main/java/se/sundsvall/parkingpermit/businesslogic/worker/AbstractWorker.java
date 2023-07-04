package se.sundsvall.parkingpermit.businesslogic.worker;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import static se.sundsvall.parkingpermit.Constants.FALSE;
import static se.sundsvall.parkingpermit.Constants.UPDATE_AVAILABLE;

abstract class AbstractWorker {
	@Autowired
	private CamundaClient camundaClient;

	protected void clearUpdateAvailable(ExternalTask externalTask) {
		/* Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), UPDATE_AVAILABLE, FALSE);
	}
}
