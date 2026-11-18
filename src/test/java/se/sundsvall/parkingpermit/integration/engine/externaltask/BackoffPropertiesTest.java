package se.sundsvall.parkingpermit.integration.engine.externaltask;

import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class BackoffPropertiesTest {

	@Autowired
	private BackoffProperties properties;

	@Autowired
	private BackoffStrategy backoffStrategy;

	@Test
	void testProperties() {
		assertThat(properties.initTime()).isEqualTo(500);
		assertThat(properties.factor()).isEqualTo(2);
		assertThat(properties.maxTime()).isEqualTo(15000);
	}

	@Test
	void contextUsesTokenEvictingBackoffStrategy() {
		assertThat(backoffStrategy).isInstanceOf(TokenEvictingBackoffStrategy.class);
	}
}
