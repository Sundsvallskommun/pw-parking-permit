package apptest;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.api.model.StartProcessResponse;

import java.time.Duration;

import static apptest.mock.Actualization.mockActualization;
import static apptest.verification.ProcessPathway.actualizationPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.executionPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static apptest.verification.ProcessPathway.handlingPathway;
import static apptest.verification.ProcessPathway.investigationPathway;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

@WireMockAppTestSuite(files = "classpath:/ProcessWithoutDeviation/", classes = Application.class)
class ProcessWithoutDeviationIT extends AbstractCamundaAppTest {

    private static final int DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS = 30;
    private static final String TENANT_ID_PARKING_PERMIT = "PARKING_PERMIT";

    @BeforeEach
    void setup() {
        setDefaultPollInterval(500, MILLISECONDS);
        setDefaultPollDelay(ZERO);
        setDefaultTimeout(Duration.ofSeconds(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS));

        await()
                .ignoreExceptions()
                .atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
                .until(() -> camundaClient.getDeployments(null, null, TENANT_ID_PARKING_PERMIT).size(), equalTo(1));
    }

    @Test
    void test001_createProcessForCitizen() throws JsonProcessingException, ClassNotFoundException {

        //Setup mocks
        //TODO rename scenarioName when all mock-helpers are created
        mockActualization("123", "create-process-for-citizen");
        //TODO add corresponding mocks for all phases.

        // Start process
        final var startResponse = setupCall()
                .withServicePath("/2281/process/start/123")
                .withHttpMethod(POST)
                .withExpectedResponseStatus(ACCEPTED)
                .sendRequest()
                .andReturnBody(StartProcessResponse.class);

        // Wait for process to finish
        awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

        // Verify wiremock stubs
        verifyAllStubs();

        // Verify process pathway.
        assertProcessPathway(startResponse.getProcessId(), Tuples.create()
                .with(tuple("Start process", "start_process"))
                .with(actualizationPathway())
                .with(tuple("Gateway isCitizen", "gateway_is_citizen"))
                .with(investigationPathway())
                .with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
                .with(decisionPathway())
                .with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
                .with(handlingPathway())
                .with(executionPathway())
                .with(followUpPathway())
                .with(tuple("Gateway closing isCitizen", "gateway_closing_is_citizen"))
                .with(tuple("End process", "end_process")));
    }
}
