package se.sundsvall.parkingpermit.integration.engine;

import feign.form.FormData;
import generated.se.sundsvall.camunda.VariableValueDto;
import java.time.OffsetDateTime;
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

	@Override
	public void deploy(final String tenantId, final String deploymentSource, final Boolean deployChangedOnly, final Boolean enableDuplicateFiltering, final String deploymentName, final OffsetDateTime deploymentActivationTime, final FormData data) {
		operatonClient.deploy(tenantId, deploymentSource, deployChangedOnly, enableDuplicateFiltering, deploymentName, deploymentActivationTime, data);
	}

	private static generated.se.sundsvall.operaton.VariableValueDto toOperatonVariableValueDto(final VariableValueDto value) {
		return new generated.se.sundsvall.operaton.VariableValueDto()
			.type(value.getType())
			.value(value.getValue())
			.valueInfo(value.getValueInfo());
	}
}
