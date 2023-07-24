package se.sundsvall.parkingpermit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.camunda.ProcessInstanceWithVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	@Mock
	private CamundaClient camundaClientMock;

	@InjectMocks
	private ProcessService processService;

	@Captor
	private ArgumentCaptor<StartProcessInstanceDto> startProcessArgumentCaptor;

	@Test
	void startProcess() {
		final var process = "process-parking-permit";
		final var tenant = "PARKING_PERMIT";
		final var businessKey = RandomStringUtils.randomNumeric(10);
		final var uuid = UUID.randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		final var processId = processService.startProcess(businessKey);

		verify(camundaClientMock).startProcessWithTenant(eq(process), eq(tenant), startProcessArgumentCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock);

		assertThat(processId).isEqualTo(uuid);
		assertThat(startProcessArgumentCaptor.getValue().getBusinessKey()).isEqualTo(businessKey);
		assertThat(startProcessArgumentCaptor.getValue().getVariables()).hasSize(1)
			.containsKey("caseNumber")
			.extractingByKey("caseNumber")
			.extracting(VariableValueDto::getType, VariableValueDto::getValue)
			.isEqualTo(List.of(ValueType.LONG.getName(), businessKey));
	}

	@Test
	void updateProcess() {
		final var uuid = UUID.randomUUID().toString();
		final var key = "updateAvailable";
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(true);

		processService.updateProcess(uuid);

		verify(camundaClientMock).setProcessInstanceVariable(uuid, key, value);
		verifyNoMoreInteractions(camundaClientMock);
	}
}
