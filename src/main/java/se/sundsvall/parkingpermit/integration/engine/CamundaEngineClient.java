package se.sundsvall.parkingpermit.integration.engine;

import generated.se.sundsvall.camunda.VariableValueDto;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

class CamundaEngineClient implements EngineClient {

	private final CamundaClient camundaClient;

	CamundaEngineClient(final CamundaClient camundaClient) {
		this.camundaClient = camundaClient;
	}

	@Override
	public void setProcessInstanceVariable(final String processInstanceId, final String variableName, final VariableValueDto value) {
		camundaClient.setProcessInstanceVariable(processInstanceId, variableName, value);
	}
}
