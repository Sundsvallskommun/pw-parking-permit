package se.sundsvall.parkingpermit.integration.engine;

import org.camunda.bpm.client.spring.impl.subscription.SpringTopicSubscriptionImpl;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import static java.util.List.of;
import static se.sundsvall.parkingpermit.Constants.TENANT_ID;

/**
 * Multiple process-worker services run in the same Operaton engine. To ensure that each worker only fetches
 * its own tasks we need to scope it to a specific TENANT_ID.
 */
@Component
class TenantAwareSubscriptions implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
		if (bean instanceof final SpringTopicSubscriptionImpl subscription) {
			subscription.getSubscriptionConfiguration().setTenantIdIn(of(TENANT_ID));
		}
		return bean;
	}
}
