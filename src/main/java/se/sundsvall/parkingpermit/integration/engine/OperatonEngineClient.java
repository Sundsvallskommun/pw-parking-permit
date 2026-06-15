package se.sundsvall.parkingpermit.integration.engine;

import generated.se.sundsvall.camunda.VariableValueDto;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;

class OperatonEngineClient implements EngineClient {

	private final OperatonClient operatonClient;

	OperatonEngineClient(final OperatonClient operatonClient) {
		this.operatonClient = operatonClient;
	}

	@Override
	public void setProcessInstanceVariable(final String processInstanceId, final String variableName, final VariableValueDto value) {
		operatonClient.setProcessInstanceVariable(processInstanceId, variableName, toOperatonVariableValueDto(value));
	}

	private static generated.se.sundsvall.operaton.VariableValueDto toOperatonVariableValueDto(final VariableValueDto value) {
		return new generated.se.sundsvall.operaton.VariableValueDto()
			.type(value.getType())
			.value(value.getValue())
			.valueInfo(value.getValueInfo());
	}
}
