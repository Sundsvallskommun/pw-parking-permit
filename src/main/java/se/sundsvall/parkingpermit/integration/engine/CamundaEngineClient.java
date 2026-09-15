package se.sundsvall.parkingpermit.integration.engine;

import feign.form.FormData;
import generated.se.sundsvall.camunda.VariableValueDto;
import java.time.OffsetDateTime;
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

	@Override
	public void deploy(final String tenantId, final String deploymentSource, final Boolean deployChangedOnly, final Boolean enableDuplicateFiltering, final String deploymentName, final OffsetDateTime deploymentActivationTime, final FormData data) {
		camundaClient.deploy(tenantId, deploymentSource, deployChangedOnly, enableDuplicateFiltering, deploymentName, deploymentActivationTime, data);
	}
}
