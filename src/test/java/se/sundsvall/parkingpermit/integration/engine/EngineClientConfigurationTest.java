package se.sundsvall.parkingpermit.integration.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EngineClientConfigurationTest {

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private OperatonClient operatonClientMock;

	private final EngineClientConfiguration configuration = new EngineClientConfiguration();

	@Test
	void selectsCamundaEngineClient() {
		assertThat(configuration.engineClient("camunda", camundaClientMock, operatonClientMock)).isInstanceOf(CamundaEngineClient.class);
	}

	@Test
	void selectsOperatonEngineClient() {
		assertThat(configuration.engineClient("operaton", camundaClientMock, operatonClientMock)).isInstanceOf(OperatonEngineClient.class);
	}

	@Test
	void throwsOnUnknownType() {
		assertThatThrownBy(() -> configuration.engineClient("unknown", camundaClientMock, operatonClientMock))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("unknown");
	}
}
