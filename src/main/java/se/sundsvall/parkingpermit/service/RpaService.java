package se.sundsvall.parkingpermit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.parkingpermit.integration.rpa.RpaClient;
import se.sundsvall.parkingpermit.integration.rpa.configuration.RpaProperties;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.parkingpermit.service.mapper.RpaMapper.toQueuesAddQueueItemParameters;

@Service
public class RpaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpaService.class);

	private static final String DUPLICATE_MESSAGE_CODE = "1016";
	private static final String DUPLICATE_MESSAGE = "Queue item already exists: {}";

	@Autowired
	RpaClient rpaClient;

	@Autowired
	RpaProperties rpaProperties;

	public void addQueueItems(List<String> queueNames, Long caseId) {
		ofNullable(queueNames).orElse(emptyList())
			.forEach(queueName -> {
				try {
					rpaClient.addQueueItem(rpaProperties.folderId(), toQueuesAddQueueItemParameters(queueName, caseId));
				} catch (ThrowableProblem e) {
					if (e.getStatus() != null && Status.CONFLICT.equals(e.getStatus()) && isDuplicateMessageCode(e.getDetail())) {
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
