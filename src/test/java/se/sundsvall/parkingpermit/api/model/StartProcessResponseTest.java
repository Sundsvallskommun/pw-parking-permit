package se.sundsvall.parkingpermit.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class StartProcessResponseTest {

	@Test
	void testBean() {
		assertThat(StartProcessResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var processInstanceId = "processInstanceId";

		final var bean = new StartProcessResponse(processInstanceId);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getProcessId()).isEqualTo(processInstanceId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new StartProcessResponse()).hasAllNullFieldsOrProperties();
	}
}
