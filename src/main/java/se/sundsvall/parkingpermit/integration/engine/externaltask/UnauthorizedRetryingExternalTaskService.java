package se.sundsvall.parkingpermit.integration.engine.externaltask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static se.sundsvall.parkingpermit.integration.engine.externaltask.OperatonExternalTaskToken.isUnauthorized;

/**
 * Retries an {@link ExternalTaskService} call once with a new token on 401, so a rejected complete does not leave the
 * task locked to be executed again. Safe to retry since the gateway rejected the call before it reached the engine.
 */
public final class UnauthorizedRetryingExternalTaskService {

	private static final Logger LOG = LoggerFactory.getLogger(UnauthorizedRetryingExternalTaskService.class);

	private UnauthorizedRetryingExternalTaskService() {}

	public static ExternalTaskService wrap(final ExternalTaskService externalTaskService, final OperatonExternalTaskToken token) {
		return (ExternalTaskService) Proxy.newProxyInstance(ExternalTaskService.class.getClassLoader(), new Class<?>[] {
			ExternalTaskService.class
		}, (proxy, method, args) -> {
			try {
				return invoke(externalTaskService, method, args);
			} catch (final RuntimeException e) {
				if (!isUnauthorized(e)) {
					throw e;
				}
				LOG.warn("External task service call '{}' was answered with 401, evicting the token and retrying", method.getName());
				token.evict();
				return invoke(externalTaskService, method, args);
			}
		});
	}

	// ExternalTaskService declares no checked exceptions, so the cause is always a RuntimeException or an Error
	private static Object invoke(final ExternalTaskService externalTaskService, final Method method, final Object[] args) {
		try {
			return method.invoke(externalTaskService, args);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof final Error error) {
				throw error;
			}
			throw (RuntimeException) e.getCause();
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
