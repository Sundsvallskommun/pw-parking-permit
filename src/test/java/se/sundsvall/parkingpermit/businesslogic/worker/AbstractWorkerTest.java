package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractWorkerTest {

	private static class Worker extends AbstractWorker {} // Test class extending the abstract class containing the clearUpdateAvailable method

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@InjectMocks
	private Worker worker;

	@Test
	void clearUpdateAvailable() {
		// Setup
		final var uuid = UUID.randomUUID().toString();
		final var key = "updateAvailable";
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

		// Mock
		when(externalTaskMock.getProcessInstanceId()).thenReturn(uuid);

		// Act
		worker.clearUpdateAvailable(externalTaskMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(uuid, key, value);
		verifyNoMoreInteractions(camundaClientMock);
	}
}
