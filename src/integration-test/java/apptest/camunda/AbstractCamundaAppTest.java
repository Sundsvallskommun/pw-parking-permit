package apptest.camunda;

import apptest.AbstractEngineAppTest;
import apptest.engine.EngineTestProperties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for the Camunda-engine integration tests: points every test in this package at a live Camunda 7 engine
 * ({@code process-engine.type=camunda}). This whole package is removed once the migration to Operaton completes.
 */
abstract class AbstractCamundaAppTest extends AbstractEngineAppTest {

	@DynamicPropertySource
	static void engine(DynamicPropertyRegistry registry) {
		EngineTestProperties.registerCamunda(registry);
	}
}
