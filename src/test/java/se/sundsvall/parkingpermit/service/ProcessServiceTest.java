package se.sundsvall.parkingpermit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@Test
	void startProcess() {
		final var process = "process-parking-permit";
		final var tenant = "PARKING_PERMIT";
		final var businessKey = RandomStringUtils.random(10);
		final var uuid = UUID.randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		final var processId = processService.startProcess(businessKey);

		assertThat(processId).isEqualTo(uuid);
		verify(camundaClientMock).startProcessWithTenant(process, tenant, new StartProcessInstanceDto().businessKey(businessKey));
		verifyNoMoreInteractions(camundaClientMock);
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
