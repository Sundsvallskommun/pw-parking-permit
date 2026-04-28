package se.sundsvall.parkingpermit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.parkingpermit.integration.relation.RelationClient;

import static org.mockito.Mockito.verify;
import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.toRelation;

@ExtendWith(MockitoExtension.class)
class RelationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

	@Mock
	private RelationClient relationClientMock;

	@InjectMocks
	private RelationService relationService;

	@Test
	void createRelation() {
		final var sourceId = "sourceId";
		final var targetId = "targetId";

		relationService.createRelation(MUNICIPALITY_ID, NAMESPACE, sourceId, targetId);

		verify(relationClientMock).createRelation(MUNICIPALITY_ID, toRelation(NAMESPACE, sourceId, targetId));
	}
}
