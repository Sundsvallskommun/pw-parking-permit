package se.sundsvall.parkingpermit.integration.camunda.mapper;

import generated.se.sundsvall.camunda.VariableValueDto;
import java.util.Map;
import java.util.Random;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.requestid.RequestId;

import static java.util.Map.entry;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.parkingpermit.Constants.*;

class CamundaMapperTest {

	@Test
	void toStartProcessInstanceDto() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var caseNumber = new Random().nextLong();

		if (isEmpty(RequestId.get())) {
			RequestId.init();
		}

		// Act
		final var dto = CamundaMapper.toStartProcessInstanceDto(municipalityId, namespace, caseNumber);

		// Assert
		assertThat(dto.getBusinessKey()).isEqualTo(String.valueOf(caseNumber));
		assertThat(dto.getVariables().entrySet()).containsExactlyInAnyOrder(
			entry(CAMUNDA_VARIABLE_MUNICIPALITY_ID, new VariableValueDto()
				.type(ValueType.STRING.getName())
				.value(municipalityId)),
			entry(CAMUNDA_VARIABLE_NAMESPACE, new VariableValueDto()
				.type(ValueType.STRING.getName())
				.value(namespace)),
			entry(CAMUNDA_VARIABLE_CASE_NUMBER, new VariableValueDto()
				.type(ValueType.LONG.getName())
				.value(caseNumber)),
			entry(CAMUNDA_VARIABLE_REQUEST_ID, new VariableValueDto()
				.type(ValueType.STRING.getName())
				.value(RequestId.get())));
	}

	@Test
	void toVariableValueDto() {
		final var value = "value";

		final var dto = CamundaMapper.toVariableValueDto(ValueType.STRING, value);

		assertThat(dto.getType()).isEqualTo(ValueType.STRING.getName());
		assertThat(dto.getValue()).isEqualTo(value);
	}

	@Test
	void toPatchVariablesDto() {
		final var key = "key";
		final var value = CamundaMapper.toVariableValueDto(ValueType.STRING, "value");
		final var dto = CamundaMapper.toPatchVariablesDto(Map.of(key, value));

		assertThat(dto.getDeletions()).isNullOrEmpty();
		assertThat(dto.getModifications()).hasSize(1).containsExactly(entry(key, value));
	}
}
