package se.sundsvall.parkingpermit.service;

import generated.se.sundsvall.camunda.PatchVariablesDto;
import generated.se.sundsvall.camunda.ProcessInstanceDto;
import generated.se.sundsvall.camunda.ProcessInstanceWithVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
import java.util.Optional;
import java.util.Random;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	@Mock
	private CamundaClient camundaClientMock;

	@InjectMocks
	private ProcessService processService;

	@Captor
	private ArgumentCaptor<StartProcessInstanceDto> startProcessArgumentCaptor;

	@Captor
	private ArgumentCaptor<PatchVariablesDto> updateProcessArgumentCaptor;

	@Test
	void startProcess() {

		// Arrange
		final var process = "process-parking-permit";
		final var tenant = "PARKING_PERMIT";
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var caseNumber = new Random().nextLong();
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			assertThat(processService.startProcess(municipalityId, namespace, caseNumber)).isEqualTo(uuid);
		}

		// Assert
		verify(camundaClientMock).startProcessWithTenant(eq(process), eq(tenant), startProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);
		assertThat(startProcessArgumentCaptor.getValue().getBusinessKey()).isEqualTo(String.valueOf(caseNumber));
		assertThat(startProcessArgumentCaptor.getValue().getVariables()).hasSize(4)
			.containsKeys("municipalityId", "namespace", "caseNumber", "requestId")
			.extractingByKeys("municipalityId", "namespace", "caseNumber", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.STRING.getName(), namespace),
				tuple(ValueType.LONG.getName(), caseNumber),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcess() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();

		when(camundaClientMock.getProcessInstance(any())).thenReturn(Optional.of(new ProcessInstanceDto()));

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			processService.updateProcess(municipalityId, namespace, uuid);
		}

		// Assert
		verify(camundaClientMock).getProcessInstance(uuid);
		verify(camundaClientMock).setProcessInstanceVariables(eq(uuid), updateProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);
		assertThat(updateProcessArgumentCaptor.getValue().getModifications()).hasSize(4)
			.containsKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extractingByKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.STRING.getName(), namespace),
				tuple(ValueType.BOOLEAN.getName(), true),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcessNotFound() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var uuid = randomUUID().toString();

		when(camundaClientMock.getProcessInstance(any())).thenReturn(empty());

		// Act
		final var result = assertThrows(org.zalando.problem.ThrowableProblem.class, () -> processService.updateProcess(municipalityId, namespace, uuid));

		// Assert
		assertThat(result)
			.hasFieldOrPropertyWithValue("status", Status.NOT_FOUND)
			.hasFieldOrPropertyWithValue("detail", "Process instance with ID '%s' does not exist!".formatted(uuid));

		// Assert
		verify(camundaClientMock).getProcessInstance(uuid);
		verify(camundaClientMock, never()).setProcessInstanceVariables(any(), any());
		verifyNoMoreInteractions(camundaClientMock);
	}
}
