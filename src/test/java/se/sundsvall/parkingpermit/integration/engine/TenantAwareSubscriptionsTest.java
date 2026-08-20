package se.sundsvall.parkingpermit.integration.engine;

import java.util.List;
import org.camunda.bpm.client.spring.SpringTopicSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.parkingpermit.Constants.TENANT_ID;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class TenantAwareSubscriptionsTest {

	@Autowired
	private List<SpringTopicSubscription> subscriptions;

	@Test
	void everySubscriptionIsScopedToTenant() {
		assertThat(subscriptions).isNotEmpty();
		assertThat(subscriptions).allSatisfy(subscription -> assertThat(subscription.getTenantIdIn())
			.as("tenant filter for topic '%s'", subscription.getTopicName())
			.isEqualTo(of(TENANT_ID)));
	}
}
