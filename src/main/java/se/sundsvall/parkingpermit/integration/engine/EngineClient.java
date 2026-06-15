package se.sundsvall.parkingpermit.integration.engine;

import generated.se.sundsvall.camunda.VariableValueDto;

/**
 * Abstraction over the process engine used by the task workers. The active implementation is selected at startup via
 * the
 * {@code process-engine.type} property ({@code camunda} or {@code operaton}), so the same worker code targets whichever
 * engine the instance is polling. The canonical variable type is the Camunda {@link VariableValueDto}; the Operaton
 * implementation converts it to its own DTO.
 */
public interface EngineClient {

	void setProcessInstanceVariable(String processInstanceId, String variableName, VariableValueDto value);
}
