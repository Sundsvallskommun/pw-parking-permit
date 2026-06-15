package se.sundsvall.parkingpermit.integration.engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class EngineClientSelectionCamundaTest {

	@Autowired
	private EngineClient engineClient;

	@Test
	void camundaEngineSelectedByDefault() {
		assertThat(engineClient).isInstanceOf(CamundaEngineClient.class);
	}
}
