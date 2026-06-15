package se.sundsvall.parkingpermit.integration.engine;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CamundaEngineClientTest {

	@Mock
	private CamundaClient camundaClientMock;

	@Test
	void delegatesToCamundaClient() {
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

		new CamundaEngineClient(camundaClientMock).setProcessInstanceVariable("processInstanceId", "variableName", value);

		verify(camundaClientMock).setProcessInstanceVariable("processInstanceId", "variableName", value);
		verifyNoMoreInteractions(camundaClientMock);
	}
}
