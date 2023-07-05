package apptest;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.sundsvall.dept44.test.AbstractAppTest;

/**
 * Test class using testcontainer to execute the process.
 * There are a lot of resources that can be added to CamundaClient
 * to make good assertions. This test class contains a few examples.
 * See Camunda API for more details https://docs.camunda.org/rest/camunda-bpm-platform/7.19/
 */
@Testcontainers
abstract class AbstractCamundaAppTest extends AbstractAppTest {

	@Container
	private static final GenericContainer<?> CAMUNDA =
		new GenericContainer<>("camunda/camunda-bpm-platform:run-7.19.0")
			.waitingFor(Wait.forHttp("/"))
			.withExposedPorts(8080);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		CAMUNDA.start();
		var camundaBaseUrl = "http://" + "localhost:" + CAMUNDA.getMappedPort(8080) + "/engine-rest";
		registry.add("integration.camunda.url", () -> camundaBaseUrl);
		registry.add("camunda.bpm.client.base-url", () -> camundaBaseUrl);
	}

	@AfterAll
	static void teardown() {
		CAMUNDA.stop();
	}
}
