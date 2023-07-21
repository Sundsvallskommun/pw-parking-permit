package se.sundsvall.parkingpermit.integration.camunda.mapper;

import static se.sundsvall.parkingpermit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;

import java.util.Map;

import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;

public class CamundaMapper {
	private CamundaMapper() {}

	public static StartProcessInstanceDto toStartProcessInstanceDto(String caseNumber) {
		return new StartProcessInstanceDto()
			.businessKey(caseNumber)
			.variables(Map.of(CAMUNDA_VARIABLE_CASE_NUMBER, toVariableValueDto(caseNumber)));
	}

	private static VariableValueDto toVariableValueDto(String caseNumber) {
		return new VariableValueDto()
			.type(Long.class.getSimpleName())
			.value(caseNumber);
	}
}
