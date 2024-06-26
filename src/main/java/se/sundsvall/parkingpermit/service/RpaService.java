package se.sundsvall.parkingpermit.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static se.sundsvall.parkingpermit.service.mapper.RpaMapper.toQueuesAddQueueItemParameters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.parkingpermit.integration.rpa.RpaClient;
import se.sundsvall.parkingpermit.integration.rpa.configuration.RpaProperties;

@Service
public class RpaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpaService.class);

	private static final String DUPLICATE_MESSAGE_CODE = "1016";
	private static final String DUPLICATE_MESSAGE = "Queue item already exists: {}";

	private final RpaClient rpaClient;

	private final RpaProperties rpaProperties;

	RpaService(RpaClient rpaClient, RpaProperties rpaProperties) {
		this.rpaClient = rpaClient;
		this.rpaProperties = rpaProperties;
	}

	public void addQueueItems(List<String> queueNames, Long caseId) {
		ofNullable(queueNames).orElse(emptyList())
			.forEach(queueName -> {
				try {
					rpaClient.addQueueItem(rpaProperties.folderId(), toQueuesAddQueueItemParameters(queueName, caseId));
				} catch (final ThrowableProblem e) {
					if (nullSafeEquals(Status.CONFLICT, e.getStatus()) && isDuplicateMessageCode(e.getDetail())) {
						// Queue item already exists
						LOGGER.warn(DUPLICATE_MESSAGE, e.getDetail());
						return;
					}
					throw e;
				}
			});
	}

	private boolean isDuplicateMessageCode(String message) {
		return ofNullable(message).orElse("").contains(DUPLICATE_MESSAGE_CODE);
	}
}
