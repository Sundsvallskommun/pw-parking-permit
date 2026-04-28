package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;

public final class RelationMapper {

	static final String SOURCE_SYSTEM = "casedata";
	static final String SOURCE_TYPE = "case";
	static final String TARGET_SYSTEM = "partyassets";
	static final String TARGET_TYPE = "asset";
	static final String TYPE = "LINK";

	private RelationMapper() {}

	public static Relation toRelation(String namespace, String sourceId, String targetId) {
		return new Relation()
			.type(TYPE)
			.source(new ResourceIdentifier()
				.namespace(namespace)
				.resourceId(sourceId)
				.service(SOURCE_SYSTEM)
				.type(SOURCE_TYPE))
			.target(new ResourceIdentifier()
				.namespace(namespace)
				.resourceId(targetId)
				.service(TARGET_SYSTEM)
				.type(TARGET_TYPE));
	}
}
