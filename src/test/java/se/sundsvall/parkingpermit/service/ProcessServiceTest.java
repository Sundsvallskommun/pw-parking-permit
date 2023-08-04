package se.sundsvall.parkingpermit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.camunda.PatchVariablesDto;
import generated.se.sundsvall.camunda.ProcessInstanceWithVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
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

		final var process = "process-parking-permit";
		final var tenant = "PARKING_PERMIT";
		final var caseNumber = RandomUtils.nextLong();
		final var uuid = UUID.randomUUID().toString();
		final var logId = UUID.randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			assertThat(processService.startProcess(caseNumber)).isEqualTo(uuid);
		}

		verify(camundaClientMock).startProcessWithTenant(eq(process), eq(tenant), startProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);

		assertThat(startProcessArgumentCaptor.getValue().getBusinessKey()).isEqualTo(String.valueOf(caseNumber));
		assertThat(startProcessArgumentCaptor.getValue().getVariables()).hasSize(2)
			.containsKeys("caseNumber", "requestId")
			.extractingByKeys("caseNumber", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(
				tuple(ValueType.LONG.getName(), caseNumber),
				tuple(ValueType.STRING.getName(), logId));
	}

	@Test
	void updateProcess() {

		final var uuid = UUID.randomUUID().toString();
		final var logId = UUID.randomUUID().toString();

		// Mock static RequestId to enable spy and to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			requestIdMock.when(RequestId::get).thenReturn(logId);

			processService.updateProcess(uuid);
		}

		verify(camundaClientMock).setProcessInstanceVariables(eq(uuid), updateProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);

		assertThat(updateProcessArgumentCaptor.getValue().getModifications()).hasSize(2)
			.containsKeys("updateAvailable", "requestId")
			.extractingByKeys("updateAvailable", "requestId")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.contains(tuple(ValueType.BOOLEAN.getName(), true), tuple(ValueType.STRING.getName(), logId));
	}
}
