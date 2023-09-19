package se.sundsvall.parkingpermit.service.mapper;

import generated.se.sundsvall.rpa.QueueItemDataDto;
import generated.se.sundsvall.rpa.QueuesAddQueueItemParameters;

import static java.util.Objects.isNull;

public class RpaMapper {

	private RpaMapper() {}

	public static QueuesAddQueueItemParameters toQueuesAddQueueItemParameters(String queueName, Long caseId) {
		return new QueuesAddQueueItemParameters().itemData(new QueueItemDataDto()
			.name(queueName)
			.reference(isNull(caseId) ? null : String.valueOf(caseId)));
	}
}
