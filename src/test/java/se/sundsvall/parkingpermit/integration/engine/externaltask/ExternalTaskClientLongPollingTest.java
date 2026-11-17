package se.sundsvall.parkingpermit.integration.engine.externaltask;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.spring.boot.starter.impl.ClientAutoConfiguration;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.WebApplicationType.NONE;

class ExternalTaskClientLongPollingTest {

	private static final String FETCH_AND_LOCK_PATH = "/engine-rest/external-task/fetchAndLock";

	@RegisterExtension
	static final WireMockExtension ENGINE = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@Test
	void fetchAndLockRequestsLongPollingFor25SecondsByDefault() {
		assertFetchAndLockSendsAsyncResponseTimeout("25000");
	}

	@Test
	void fetchAndLockRequestsLongPollingFor2SecondsInProcessTests() {
		assertFetchAndLockSendsAsyncResponseTimeout("2000", "it");
	}

	// A real SpringApplication is needed since the starter opens its subscriptions on ApplicationStartedEvent
	private static void assertFetchAndLockSendsAsyncResponseTimeout(final String expectedTimeout, final String... profiles) {
		ENGINE.stubFor(post(urlPathEqualTo(FETCH_AND_LOCK_PATH)).willReturn(okJson("[]")));

		try (final var context = new SpringApplicationBuilder(ClientAutoConfiguration.class, SubscriptionConfiguration.class)
			.web(NONE)
			.profiles(profiles)
			.run("--camunda.bpm.client.base-url=" + ENGINE.baseUrl() + "/engine-rest")) {

			await().atMost(10, SECONDS).untilAsserted(() -> ENGINE.verify(postRequestedFor(urlPathEqualTo(FETCH_AND_LOCK_PATH))
				.withRequestBody(matchingJsonPath("$.asyncResponseTimeout", equalTo(expectedTimeout)))));
		}
	}

	@Configuration
	static class SubscriptionConfiguration {

		@Bean
		@ExternalTaskSubscription("long-polling-test")
		ExternalTaskHandler longPollingTestHandler() {
			return (externalTask, externalTaskService) -> {};
		}
	}
}
