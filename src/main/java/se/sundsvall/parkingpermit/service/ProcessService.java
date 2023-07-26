package se.sundsvall.parkingpermit.service;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.PROCESS_KEY;
import static se.sundsvall.parkingpermit.Constants.TENANTID_TEMPLATE;
import static se.sundsvall.parkingpermit.Constants.TRUE;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toPatchVariablesDto;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toStartProcessInstanceDto;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toVariableValueDto;

import java.util.Map;

import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

@Service
public class ProcessService {
	@Autowired
	private CamundaClient client;

	public String startProcess(String caseNumber) {
		return client.startProcessWithTenant(PROCESS_KEY, TENANTID_TEMPLATE, toStartProcessInstanceDto(caseNumber)).getId();
	}

	public void updateProcess(String processInstanceId) {
		final var variablesToUpdate = Map.of(
			CAMUNDA_VARIABLE_UPDATE_AVAILABLE, TRUE,
			CAMUNDA_VARIABLE_REQUEST_ID, toVariableValueDto(ValueType.STRING, RequestId.get()));

		client.setProcessInstanceVariables(processInstanceId, toPatchVariablesDto(variablesToUpdate));
	}
}
