package se.sundsvall.parkingpermit.businesslogic.handler;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

@Component
public class FailureHandler {

	@Value("${camunda.worker.max.retries}")
	private int maxRetries;

	@Value("${camunda.worker.retry.timeout}")
	private long retryTimeout;

	public void handleException(ExternalTaskService externalTaskService, ExternalTask externalTask, String message) {
		externalTaskService.handleFailure(externalTask.getId(), externalTask.getWorkerId(),
			message,
			calculateRetries(externalTask),
			retryTimeout);
	}

	public void handleException(ExternalTaskService externalTaskService, ExternalTask externalTask, String message, Map<String, Object> variables) {
		externalTaskService.handleFailure(externalTask.getId(), externalTask.getWorkerId(),
			message,
			calculateRetries(externalTask),
			retryTimeout,
			variables,
			emptyMap());
	}

	private int calculateRetries(ExternalTask externalTask) {
		return Optional.ofNullable(externalTask.getRetries())
			.map(retries -> retries - 1)
			.orElse(maxRetries);
	}
}
