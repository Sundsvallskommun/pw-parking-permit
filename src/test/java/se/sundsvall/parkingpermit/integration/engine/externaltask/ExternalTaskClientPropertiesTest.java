package se.sundsvall.parkingpermit.integration.engine.externaltask;

import java.io.IOException;
import org.camunda.bpm.client.spring.boot.starter.ClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ExternalTaskClientPropertiesTest {

	@Autowired
	private ClientProperties clientProperties;

	@Test
	void longPollingTimeoutIs25Seconds() {
		assertThat(clientProperties.getAsyncResponseTimeout()).isEqualTo(25000L);
	}

	@Test
	void itProfileKeepsLongPollingTimeoutAndBackoffBelowTheProcessTestAwait() throws IOException {
		final var propertySource = new YamlPropertySourceLoader().load("it", new ClassPathResource("application-it.yaml")).getFirst();

		assertThat(propertySource.getProperty("camunda.bpm.client.async-response-timeout")).hasToString("2000");
		assertThat(propertySource.getProperty("camunda.bpm.client.backoff.maxTime")).hasToString("2000");
	}
}
