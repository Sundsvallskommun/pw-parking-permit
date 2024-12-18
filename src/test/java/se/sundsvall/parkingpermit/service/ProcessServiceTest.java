package se.sundsvall.parkingpermit.service;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.camunda.PatchVariablesDto;
import generated.se.sundsvall.camunda.ProcessInstanceWithVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
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
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

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
		final var caseNumber = new Random().nextLong();
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			assertThat(processService.startProcess(municipalityId, caseNumber)).isEqualTo(uuid);
		}

		// Assert
		verify(camundaClientMock).startProcessWithTenant(eq(process), eq(tenant), startProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);
		assertThat(startProcessArgumentCaptor.getValue().getBusinessKey()).isEqualTo(String.valueOf(caseNumber));
		assertThat(startProcessArgumentCaptor.getValue().getVariables()).hasSize(3)
			.containsKeys("municipalityId", "caseNumber", "requestId")
			.extractingByKeys("municipalityId", "caseNumber", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.LONG.getName(), caseNumber),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcess() {

		// Arrange
		final var municipalityId = "2281";
		final var uuid = randomUUID().toString();
		final var logId = randomUUID().toString();

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			// Act
			processService.updateProcess(municipalityId, uuid);
		}

		// Assert
		verify(camundaClientMock).setProcessInstanceVariables(eq(uuid), updateProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);
		assertThat(updateProcessArgumentCaptor.getValue().getModifications()).hasSize(3)
			.containsKeys("municipalityId", "updateAvailable", "requestId")
			.extractingByKeys("municipalityId", "updateAvailable", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(
				tuple(ValueType.STRING.getName(), municipalityId),
				tuple(ValueType.BOOLEAN.getName(), true),
				tuple(ValueType.STRING.getName(), logId));
	}
}
