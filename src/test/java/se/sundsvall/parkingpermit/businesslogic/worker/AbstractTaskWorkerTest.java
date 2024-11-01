package se.sundsvall.parkingpermit.businesslogic.worker;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.Constants;
import se.sundsvall.parkingpermit.businesslogic.handler.FailureHandler;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.casedata.CaseDataClient;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractTaskWorkerTest {

	private static class Worker extends AbstractTaskWorker { // Test class extending the abstract class containing the clearUpdateAvailable method

		Worker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
			super(camundaClient, caseDataClient, failureHandler);
		}

		@Override
		public void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
			// Do nothing
		}
	}

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

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

	@Test
	void execute() {
		final var requestId = UUID.randomUUID().toString();

		when(externalTaskMock.getVariable(Constants.CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(requestId);

		// Mock static RequestId to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			// Act
			worker.execute(externalTaskMock, externalTaskServiceMock);

			// Verify static method
			requestIdMock.verify(() -> RequestId.init(requestId));
		}
	}
}
