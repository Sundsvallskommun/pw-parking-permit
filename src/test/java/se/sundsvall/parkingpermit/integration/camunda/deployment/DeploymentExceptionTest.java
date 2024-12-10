package se.sundsvall.parkingpermit.integration.camunda.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeploymentExceptionTest {

	@Test
	void testInheritance() {
		assertThat(DeploymentException.class).hasSuperclass(RuntimeException.class);
	}
}
