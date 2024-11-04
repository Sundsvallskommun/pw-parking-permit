package se.sundsvall.parkingpermit.service;

import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import java.util.Map;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.PROCESS_KEY;
import static se.sundsvall.parkingpermit.Constants.TENANTID_TEMPLATE;
import static se.sundsvall.parkingpermit.Constants.TRUE;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toPatchVariablesDto;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toStartProcessInstanceDto;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.toVariableValueDto;


@Service
public class ProcessService {

	private final CamundaClient camundaClient;

	ProcessService(CamundaClient camundaClient) {
		this.camundaClient = camundaClient;
	}

	public String startProcess(String municipalityId, Long caseNumber) {
		return camundaClient.startProcessWithTenant(PROCESS_KEY, TENANTID_TEMPLATE, toStartProcessInstanceDto(municipalityId, caseNumber)).getId();
	}

	public void updateProcess(String municipalityId, String processInstanceId) {
		final var variablesToUpdate = Map.of(
			CAMUNDA_VARIABLE_MUNICIPALITY_ID, toVariableValueDto(ValueType.STRING, municipalityId),
			CAMUNDA_VARIABLE_UPDATE_AVAILABLE, TRUE,
			CAMUNDA_VARIABLE_REQUEST_ID, toVariableValueDto(ValueType.STRING, RequestId.get()));

		camundaClient.setProcessInstanceVariables(processInstanceId, toPatchVariablesDto(variablesToUpdate));
	}
}
