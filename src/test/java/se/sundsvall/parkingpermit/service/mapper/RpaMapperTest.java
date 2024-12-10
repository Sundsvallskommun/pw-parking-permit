package se.sundsvall.parkingpermit.service.mapper;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.parkingpermit.service.mapper.RpaMapper.toQueuesAddQueueItemParameters;

import generated.se.sundsvall.rpa.QueueItemDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RpaMapperTest {

	@Test
	@DisplayName("Should return an empty QueuesAddQueueItemParameters instance")
	void testWithNull() {
		assertThat(toQueuesAddQueueItemParameters(null, null).getItemData())
			.isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("specificContent")
			.extracting("specificContent")
			.isEqualTo(emptyMap());
	}

	@Test
	@DisplayName("Should return a QueuesAddQueueItemParameters instance with set attributes")
	void testWithValues() {
		final var id = Long.valueOf(456L);
		final var queueName = "queueName";

		assertThat(toQueuesAddQueueItemParameters(queueName, id).getItemData())
			.isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("name", "reference", "specificContent")
			.extracting(QueueItemDataDto::getName, QueueItemDataDto::getReference, QueueItemDataDto::getSpecificContent)
			.containsExactlyInAnyOrder(queueName, String.valueOf(id), emptyMap());
	}
}
