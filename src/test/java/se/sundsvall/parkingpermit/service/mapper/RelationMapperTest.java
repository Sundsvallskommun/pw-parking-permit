package se.sundsvall.parkingpermit.service.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.SOURCE_SYSTEM;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.SOURCE_TYPE;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.TARGET_SYSTEM;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.TARGET_TYPE;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.TYPE;

class RelationMapperTest {

	private static final String NAMESPACE = "test-namespace";
	private static final String SOURCE_ID = "source-id";
	private static final String TARGET_ID = "target-id";

	@Test
	void toRelation() {
		final var relation = RelationMapper.toRelation(NAMESPACE, SOURCE_ID, TARGET_ID);

		assertThat(relation).isNotNull();
		assertThat(relation.getType()).isEqualTo(TYPE);
		assertThat(relation.getSource()).isNotNull();
		assertThat(relation.getSource().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(relation.getSource().getResourceId()).isEqualTo(SOURCE_ID);
		assertThat(relation.getSource().getService()).isEqualTo(SOURCE_SYSTEM);
		assertThat(relation.getSource().getType()).isEqualTo(SOURCE_TYPE);
		assertThat(relation.getTarget()).isNotNull();
		assertThat(relation.getTarget().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(relation.getTarget().getResourceId()).isEqualTo(TARGET_ID);
		assertThat(relation.getTarget().getService()).isEqualTo(TARGET_SYSTEM);
		assertThat(relation.getTarget().getType()).isEqualTo(TARGET_TYPE);
	}
}
