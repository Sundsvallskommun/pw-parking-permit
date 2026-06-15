package se.sundsvall.parkingpermit.service;

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
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private OperatonClient operatonClientMock;

	@InjectMocks
	private ProcessService processService;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.operaton.StartProcessInstanceDto> startProcessArgumentCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.operaton.PatchVariablesDto> operatonPatchVariablesCaptor;

	@Captor
	private ArgumentCaptor<generated.se.sundsvall.camunda.PatchVariablesDto> camundaPatchVariablesCaptor;

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
		final var processInstance = new generated.se.sundsvall.operaton.ProcessInstanceWithVariablesDto().id(uuid);

		when(operatonClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			assertThat(processService.startProcess(municipalityId, namespace, caseNumber)).isEqualTo(uuid);
		}

		// Assert - new processes go to Operaton, Camunda is untouched
		verify(operatonClientMock).startProcessWithTenant(eq(process), eq(tenant), startProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(operatonClientMock);
		verifyNoInteractions(camundaClientMock);
		assertThat(startProcessArgumentCaptor.getValue().getBusinessKey()).isEqualTo(String.valueOf(caseNumber));
		assertThat(startProcessArgumentCaptor.getValue().getVariables()).hasSize(4)
			.containsKeys("municipalityId", "namespace", "caseNumber", "requestId")
			.extractingByKeys("municipalityId", "namespace", "caseNumber", "requestId")
			.extracting(generated.se.sundsvall.operaton.VariableValueDto::getType, generated.se.sundsvall.operaton.VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.STRING.getName(), namespace),
				tuple(ValueType.LONG.getName(), caseNumber),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcessFoundInOperaton() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();

		when(operatonClientMock.getProcessInstance(any())).thenReturn(of(new generated.se.sundsvall.operaton.ProcessInstanceDto()));

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			processService.updateProcess(municipalityId, namespace, uuid);
		}

		// Assert - process exists in Operaton, so it is updated there and Camunda is never queried
		verify(operatonClientMock).getProcessInstance(uuid);
		verify(operatonClientMock).setProcessInstanceVariables(eq(uuid), operatonPatchVariablesCaptor.capture());
		verifyNoMoreInteractions(operatonClientMock);
		verifyNoInteractions(camundaClientMock);
		assertThat(operatonPatchVariablesCaptor.getValue().getModifications()).hasSize(4)
			.containsKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extractingByKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extracting(generated.se.sundsvall.operaton.VariableValueDto::getType, generated.se.sundsvall.operaton.VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.STRING.getName(), namespace),
				tuple(ValueType.BOOLEAN.getName(), true),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcessFoundInCamunda() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();

		when(operatonClientMock.getProcessInstance(any())).thenReturn(empty());
		when(camundaClientMock.getProcessInstance(any())).thenReturn(of(new generated.se.sundsvall.camunda.ProcessInstanceDto()));

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			processService.updateProcess(municipalityId, namespace, uuid);
		}

		// Assert - process is missing in Operaton, so the update falls back to Camunda
		verify(operatonClientMock).getProcessInstance(uuid);
		verify(operatonClientMock, never()).setProcessInstanceVariables(any(), any());
		verify(camundaClientMock).getProcessInstance(uuid);
		verify(camundaClientMock).setProcessInstanceVariables(eq(uuid), camundaPatchVariablesCaptor.capture());
		verifyNoMoreInteractions(operatonClientMock, camundaClientMock);
		assertThat(camundaPatchVariablesCaptor.getValue().getModifications()).hasSize(4)
			.containsKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extractingByKeys("municipalityId", "namespace", "updateAvailable", "requestId")
			.extracting(generated.se.sundsvall.camunda.VariableValueDto::getType, generated.se.sundsvall.camunda.VariableValueDto::getValue)
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

		when(operatonClientMock.getProcessInstance(any())).thenReturn(empty());
		when(camundaClientMock.getProcessInstance(any())).thenReturn(empty());

		// Act
		final var result = assertThrows(se.sundsvall.dept44.problem.ThrowableProblem.class, () -> processService.updateProcess(municipalityId, namespace, uuid));

		// Assert - process exists in neither engine
		assertThat(result)
			.hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
			.hasFieldOrPropertyWithValue("detail", "Process instance with ID '%s' does not exist!".formatted(uuid));

		verify(operatonClientMock).getProcessInstance(uuid);
		verify(camundaClientMock).getProcessInstance(uuid);
		verify(operatonClientMock, never()).setProcessInstanceVariables(any(), any());
		verify(camundaClientMock, never()).setProcessInstanceVariables(any(), any());
		verifyNoMoreInteractions(operatonClientMock, camundaClientMock);
	}
}
