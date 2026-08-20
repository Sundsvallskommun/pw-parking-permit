package se.sundsvall.parkingpermit.service;

import java.util.Map;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.camunda.mapper.CamundaMapper;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;
import se.sundsvall.parkingpermit.integration.operaton.mapper.OperatonMapper;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.parkingpermit.Constants.PROCESS_KEY;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_NAMESPACE;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_REQUEST_ID;
import static se.sundsvall.parkingpermit.Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.parkingpermit.Constants.TENANT_ID;
import static se.sundsvall.parkingpermit.Constants.TRUE;

@Service
public class ProcessService {

	private final CamundaClient camundaClient;

	private final OperatonClient operatonClient;

	ProcessService(CamundaClient camundaClient, OperatonClient operatonClient) {
		this.camundaClient = camundaClient;
		this.operatonClient = operatonClient;
	}

	public String startProcess(final String municipalityId, final String namespace, final Long caseNumber) {
		// New processes are always created in Operaton.
		return operatonClient.startProcessWithTenant(PROCESS_KEY, TENANT_ID, OperatonMapper.toStartProcessInstanceDto(municipalityId, namespace, caseNumber)).getId();
	}

	public void updateProcess(final String municipalityId, final String namespace, final String processInstanceId) {
		// New processes live in Operaton, older ones still in Camunda. Probe Operaton first and fall back to Camunda.
		if (operatonClient.getProcessInstance(processInstanceId).isPresent()) {
			operatonClient.setProcessInstanceVariables(processInstanceId, operatonUpdateVariables(municipalityId, namespace));
		} else if (camundaClient.getProcessInstance(processInstanceId).isPresent()) {
			camundaClient.setProcessInstanceVariables(processInstanceId, camundaUpdateVariables(municipalityId, namespace));
		} else {
			throw Problem.valueOf(NOT_FOUND, "Process instance with ID '%s' does not exist!".formatted(processInstanceId));
		}
	}

	private generated.se.sundsvall.operaton.PatchVariablesDto operatonUpdateVariables(final String municipalityId, final String namespace) {
		return OperatonMapper.toPatchVariablesDto(Map.of(
			PROCESS_VARIABLE_MUNICIPALITY_ID, OperatonMapper.toVariableValueDto(ValueType.STRING, municipalityId),
			PROCESS_VARIABLE_NAMESPACE, OperatonMapper.toVariableValueDto(ValueType.STRING, namespace),
			PROCESS_VARIABLE_UPDATE_AVAILABLE, OperatonMapper.toVariableValueDto(ValueType.BOOLEAN, true),
			PROCESS_VARIABLE_REQUEST_ID, OperatonMapper.toVariableValueDto(ValueType.STRING, RequestId.get())));
	}

	private generated.se.sundsvall.camunda.PatchVariablesDto camundaUpdateVariables(final String municipalityId, final String namespace) {
		return CamundaMapper.toPatchVariablesDto(Map.of(
			PROCESS_VARIABLE_MUNICIPALITY_ID, CamundaMapper.toVariableValueDto(ValueType.STRING, municipalityId),
			PROCESS_VARIABLE_NAMESPACE, CamundaMapper.toVariableValueDto(ValueType.STRING, namespace),
			PROCESS_VARIABLE_UPDATE_AVAILABLE, TRUE,
			PROCESS_VARIABLE_REQUEST_ID, CamundaMapper.toVariableValueDto(ValueType.STRING, RequestId.get())));
	}
}
