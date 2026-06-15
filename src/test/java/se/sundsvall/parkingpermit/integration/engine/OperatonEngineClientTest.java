package se.sundsvall.parkingpermit.integration.engine;

import generated.se.sundsvall.camunda.VariableValueDto;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.Map;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OperatonEngineClientTest {

	@Mock
	private OperatonClient operatonClientMock;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.operaton.VariableValueDto> valueCaptor;

	@Test
	void convertsToOperatonDtoAndDelegates() {
		final var valueInfo = Map.<String, Object>of("info", "data");
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false).valueInfo(valueInfo);

		new OperatonEngineClient(operatonClientMock).setProcessInstanceVariable("processInstanceId", "variableName", value);

		verify(operatonClientMock).setProcessInstanceVariable(eq("processInstanceId"), eq("variableName"), valueCaptor.capture());
		verifyNoMoreInteractions(operatonClientMock);
		assertThat(valueCaptor.getValue().getType()).isEqualTo(ValueType.BOOLEAN.getName());
		assertThat(valueCaptor.getValue().getValue()).isEqualTo(false);
		assertThat(valueCaptor.getValue().getValueInfo()).isEqualTo(valueInfo);
	}

	@Test
	void deployDelegatesToOperatonClient() {
		final var data = new File("process.bpmn");
		final var activationTime = OffsetDateTime.now();

		new OperatonEngineClient(operatonClientMock).deploy("tenantId", "deploymentSource", true, true, "deploymentName", activationTime, data);

		verify(operatonClientMock).deploy("tenantId", "deploymentSource", true, true, "deploymentName", activationTime, data);
		verifyNoMoreInteractions(operatonClientMock);
	}
}
