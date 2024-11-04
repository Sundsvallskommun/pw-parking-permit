package apptest.mock.api;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class ApiGateway {

    public static void mockApiGatewayToken() {
        stubFor(post(urlEqualTo("/api-gateway/token"))
                .withBasicAuth("the-client-id", "the-client-secret")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("common/responses/api-gateway-retrieve-token.json")));
    }
}
