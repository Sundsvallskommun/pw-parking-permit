package se.sundsvall.parkingpermit.integration.camunda.mapper;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;

import java.util.Map;

import org.camunda.bpm.engine.variable.type.ValueType;

import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;

public class CamundaMapper {
	private CamundaMapper() {}

	public static StartProcessInstanceDto toStartProcessInstanceDto(String caseNumber) {
		return new StartProcessInstanceDto()
			.businessKey(caseNumber)
			.variables(Map.of(CAMUNDA_VARIABLE_CASE_NUMBER, toVariableValueDto(ValueType.LONG, caseNumber)));
	}

	private static VariableValueDto toVariableValueDto(ValueType valueType, String caseNumber) {
		return new VariableValueDto()
			.type(valueType.getName())
			.value(caseNumber);
	}
}
