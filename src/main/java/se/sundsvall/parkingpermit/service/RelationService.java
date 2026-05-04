package se.sundsvall.parkingpermit.service;

import org.springframework.stereotype.Service;
import se.sundsvall.parkingpermit.integration.relation.RelationClient;

import static se.sundsvall.parkingpermit.service.mapper.RelationMapper.toRelation;

@Service
public class RelationService {

	private final RelationClient relationClient;

	public RelationService(RelationClient relationClient) {
		this.relationClient = relationClient;
	}

	public void createRelation(String municipalityId, String namespace, String sourceId, String targetId) {
		relationClient.createRelation(municipalityId, toRelation(namespace, sourceId, targetId));
	}
}
