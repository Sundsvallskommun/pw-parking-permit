package se.sundsvall.parkingpermit.integration.camunda.mapper;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

import java.util.Map;

import org.camunda.bpm.engine.variable.type.ValueType;

import generated.se.sundsvall.camunda.PatchVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
import se.sundsvall.dept44.requestid.RequestId;

public class CamundaMapper {
	private CamundaMapper() {}

	public static StartProcessInstanceDto toStartProcessInstanceDto(String caseNumber) {
		return new StartProcessInstanceDto()
			.businessKey(caseNumber)
			.variables(Map.of(
				CAMUNDA_VARIABLE_CASE_NUMBER, toVariableValueDto(ValueType.LONG, caseNumber),
				CAMUNDA_VARIABLE_REQUEST_ID, toVariableValueDto(ValueType.STRING, RequestId.get())));
	}

	public static VariableValueDto toVariableValueDto(ValueType valueType, String value) {
		return new VariableValueDto()
			.type(valueType.getName())
			.value(value);
	}

	public static PatchVariablesDto toPatchVariablesDto(Map<String, VariableValueDto> variablesToUpdate) {
		return new PatchVariablesDto()
			.modifications(variablesToUpdate);
	}
}
