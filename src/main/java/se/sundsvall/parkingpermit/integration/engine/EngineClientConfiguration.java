package se.sundsvall.parkingpermit.integration.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.operaton.OperatonClient;

/**
 * Single place that decides which process engine the task workers target. The {@code process-engine.type} property
 * selects the implementation at startup; an unknown value fails fast on boot instead of producing a missing-bean error.
 */
@Configuration
public class EngineClientConfiguration {

	@Bean
	EngineClient engineClient(@Value("${process-engine.type:camunda}") final String type, final CamundaClient camundaClient, final OperatonClient operatonClient) {
		return switch (type.toLowerCase()) {
			case "camunda" -> new CamundaEngineClient(camundaClient);
			case "operaton" -> new OperatonEngineClient(operatonClient);
			default -> throw new IllegalStateException("Unknown process-engine.type: '%s' (expected 'camunda' or 'operaton')".formatted(type));
		};
	}
}
