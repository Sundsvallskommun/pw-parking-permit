package se.sundsvall.parkingpermit.integration.engine.externaltask;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

class ExternalTaskClientUnauthorizedTest {

	private static final String TOKEN_PATH = "/token";
	private static final String FETCH_AND_LOCK_PATH = "/engine-rest/external-task/fetchAndLock";
	private static final String COMPLETE_PATH = "/engine-rest/external-task/task-1/complete";
	private static final String TOPIC = "unauthorized-test";
	private static final String WSO2_INVALID_CREDENTIALS = """
		{"code":"900901","message":"Invalid Credentials","description":"Invalid Credentials. Make sure you have provided the correct security credentials"}""";
	private static final String ONE_TASK = """
		[{"id":"task-1","topicName":"%s","workerId":"worker","processInstanceId":"process-1","variables":{}}]""".formatted(TOPIC);

	@RegisterExtension
	static final WireMockExtension WIREMOCK = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@Test
	void fetchAndLockAnsweredWith401EvictsTheTokenAndNextPollUsesANewOne() {
		stubTokens();
		WIREMOCK.stubFor(fetchAndLockWith("token-1").willReturn(unauthorized()));
		WIREMOCK.stubFor(fetchAndLockWith("token-2").willReturn(okJson("[]")));

		runClient((externalTask, externalTaskService) -> {}, () -> WIREMOCK.verify(postRequestedFor(urlPathEqualTo(FETCH_AND_LOCK_PATH))
			.withHeader(AUTHORIZATION, equalTo("Bearer token-2"))));
	}

	@Test
	void completeAnsweredWith401IsRetriedWithANewToken() {
		stubTokens();
		WIREMOCK.stubFor(fetchAndLockWith("token-1").willReturn(okJson(ONE_TASK)));
		WIREMOCK.stubFor(completeWith("token-1").willReturn(unauthorized()));
		WIREMOCK.stubFor(completeWith("token-2").willReturn(aResponse().withStatus(204)));

		final var token = createToken();
		runClient(token, (externalTask, externalTaskService) -> UnauthorizedRetryingExternalTaskService.wrap(externalTaskService, token).complete(externalTask),
			() -> WIREMOCK.verify(postRequestedFor(urlPathEqualTo(COMPLETE_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer token-2"))));
	}

	private static void runClient(final ExternalTaskHandler handler, final Runnable assertion) {
		runClient(createToken(), handler, assertion);
	}

	private static void runClient(final OperatonExternalTaskToken token, final ExternalTaskHandler handler, final Runnable assertion) {
		final var client = ExternalTaskClient.create()
			.baseUrl(WIREMOCK.baseUrl() + "/engine-rest")
			.addInterceptor(new OperatonExternalTaskAuthInterceptor(token))
			.backoffStrategy(new TokenEvictingBackoffStrategy(10, 2, 100, token))
			.build();
		try {
			client.subscribe(TOPIC).handler(handler).open();
			await().atMost(10, SECONDS).untilAsserted(assertion::run);
		} finally {
			client.stop();
		}
	}

	private static OperatonExternalTaskToken createToken() {
		return new OperatonExternalTaskToken(new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("operaton")
			.clientId("the-client-id")
			.clientSecret("the-client-secret")
			.authorizationGrantType(CLIENT_CREDENTIALS)
			.tokenUri(WIREMOCK.baseUrl() + TOKEN_PATH)
			.build()));
	}

	private static void stubTokens() {
		WIREMOCK.stubFor(tokenResponse(STARTED, "token-2", "token-1"));
		WIREMOCK.stubFor(tokenResponse("token-2", "token-3", "token-2"));
		WIREMOCK.stubFor(tokenResponse("token-3", "token-3", "token-3"));
	}

	private static MappingBuilder tokenResponse(final String state, final String nextState, final String accessToken) {
		return post(urlPathEqualTo(TOKEN_PATH))
			.inScenario("tokens")
			.whenScenarioStateIs(state)
			.willSetStateTo(nextState)
			.willReturn(okJson("""
				{"access_token":"%s","token_type":"Bearer","expires_in":3600}""".formatted(accessToken)));
	}

	private static MappingBuilder fetchAndLockWith(final String accessToken) {
		return post(urlPathEqualTo(FETCH_AND_LOCK_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer " + accessToken));
	}

	private static MappingBuilder completeWith(final String accessToken) {
		return post(urlPathEqualTo(COMPLETE_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer " + accessToken));
	}

	private static ResponseDefinitionBuilder unauthorized() {
		return aResponse().withStatus(401).withHeader("Content-Type", "application/json").withBody(WSO2_INVALID_CREDENTIALS);
	}
}
