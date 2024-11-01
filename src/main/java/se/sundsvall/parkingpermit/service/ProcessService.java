package se.sundsvall.parkingpermit.service;

import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import java.util.Map;

import static se.sundsvall.parkingpermit.Constants.*;
import static se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper.*;

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
