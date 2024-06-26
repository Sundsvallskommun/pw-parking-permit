package se.sundsvall.parkingpermit.service.mapper;

import static java.util.Optional.ofNullable;

import generated.se.sundsvall.rpa.QueueItemDataDto;
import generated.se.sundsvall.rpa.QueuesAddQueueItemParameters;

public final class RpaMapper {

	private RpaMapper() {}

	public static QueuesAddQueueItemParameters toQueuesAddQueueItemParameters(String queueName, Long caseId) {
		return new QueuesAddQueueItemParameters().itemData(new QueueItemDataDto()
			.name(queueName)
			.reference(ofNullable(caseId).map(String::valueOf).orElse(null)));
	}
}
