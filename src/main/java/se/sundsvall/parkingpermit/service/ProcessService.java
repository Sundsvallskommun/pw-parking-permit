package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import static se.sundsvall.parkingpermit.Constants.PROCESS_KEY;
import static se.sundsvall.parkingpermit.Constants.TENANTID_TEMPLATE;
import static se.sundsvall.parkingpermit.Constants.TRUE;
import static se.sundsvall.parkingpermit.Constants.UPDATE_AVAILABLE;

@Service
public class ProcessService {

	@Autowired
	private CamundaClient client;

	public String startProcess(String businessKey) {
		return client.startProcessWithTenant(PROCESS_KEY, TENANTID_TEMPLATE, new StartProcessInstanceDto().businessKey(businessKey)).getId();
	}

	public void updateProcess(String processInstanceId) {
		client.setProcessInstanceVariable(processInstanceId, UPDATE_AVAILABLE, TRUE);
	}
}
