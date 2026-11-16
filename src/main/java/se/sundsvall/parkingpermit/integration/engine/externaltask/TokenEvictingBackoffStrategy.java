package se.sundsvall.parkingpermit.integration.engine.externaltask;

import java.util.List;
import org.camunda.bpm.client.backoff.ExponentialErrorBackoffStrategy;
import org.camunda.bpm.client.exception.ExternalTaskClientException;
import org.camunda.bpm.client.task.ExternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static se.sundsvall.parkingpermit.integration.engine.externaltask.OperatonExternalTaskToken.isUnauthorized;

/**
 * Backs off only after a failed fetch, and evicts the token when fetchAndLock is answered with 401 so the next poll
 * obtains a new one.
 */
class TokenEvictingBackoffStrategy extends ExponentialErrorBackoffStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(TokenEvictingBackoffStrategy.class);

	private final OperatonExternalTaskToken token;

	TokenEvictingBackoffStrategy(final long initTime, final float factor, final long maxTime, final OperatonExternalTaskToken token) {
		super(initTime, factor, maxTime);
		this.token = token;
	}

	@Override
	public void reconfigure(final List<ExternalTask> externalTasks, final ExternalTaskClientException exception) {
		if (isUnauthorized(exception)) {
			LOG.warn("Fetch and lock was answered with 401, evicting the external task client token");
			token.evict();
		}
		super.reconfigure(externalTasks, exception);
	}
}
