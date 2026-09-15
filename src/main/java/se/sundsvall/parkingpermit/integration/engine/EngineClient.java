package se.sundsvall.parkingpermit.integration.engine;

import feign.form.FormData;
import generated.se.sundsvall.camunda.VariableValueDto;
import java.time.OffsetDateTime;

/**
 * Abstraction over the process engine targeted by this instance. The active implementation is selected at startup via
 * the
 * {@code process-engine.type} property ({@code camunda} or {@code operaton}), so both the task workers and the startup
 * auto-deployment target whichever engine the instance is polling. The canonical variable type is the Camunda
 * {@link VariableValueDto}; the Operaton implementation converts it to its own DTO.
 */
public interface EngineClient {

	void setProcessInstanceVariable(String processInstanceId, String variableName, VariableValueDto value);

	void deploy(String tenantId, String deploymentSource, Boolean deployChangedOnly, Boolean enableDuplicateFiltering, String deploymentName, OffsetDateTime deploymentActivationTime, FormData data);
}
