package com.coltrip.backend.alternative.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiAlternativeClientTest {

    private MockRestServiceServer server;
    private AiAlternativeClient client;
    private final AiAlternativeClient.Request request =
            new AiAlternativeClient.Request("126081", 13, true, 65.4);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiAlternativeClient(builder.build(),
                new AiProperties("https://ai.invalid", "test-key", "real", 3000, 30000));
    }

    @Test
    void sendsAuthenticatedCamelCaseRequestAndKeepsDecimals() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andExpect(content().string(containsString("\"poiId\":\"126081\"")))
                .andExpect(content().string(containsString("\"isWeekend\":true")))
                .andExpect(content().string(containsString("\"baselineQuietIndex\":65.4")))
                .andRespond(withSuccess("""
                        {"triggered":true,"targetQuietIndex":35.2,"alternatives":[
                          {"poiId":"126078","name":"Beach","quietIndex":80.7,
                           "distanceKm":2.1,"score":0.85,"recommendReason":"Quieter"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var response = client.fetch(request);

        assertEquals(35.2, response.targetQuietIndex());
        assertEquals(80.7, response.alternatives().getFirst().quietIndex());
        server.verify();
    }

    @Test
    void acceptsNoTriggerAndNullBaseline() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withSuccess("""
                        {"triggered":false,"targetQuietIndex":80.1,"alternatives":[]}
                        """, MediaType.APPLICATION_JSON));
        var result = client.fetch(new AiAlternativeClient.Request("126081", 12, false, null));
        assertFalse(result.triggered());
        assertTrue(result.alternatives().isEmpty());
        server.verify();
    }

    @Test
    void upstreamFailureIsNotAnEmptyRecommendation() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        var error = assertThrows(AiIntegrationException.class, () -> client.fetch(request));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        server.verify();
    }

    @Test
    void upstreamAuthenticationFailureIsNotUserUnauthorized() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(AiIntegrationException.class, () -> client.fetch(request)).getStatus());
        server.verify();
    }

    @Test
    void distinguishesTimeoutFromConnectionFailure() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withException(new SocketTimeoutException("timeout")));
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withException(new IOException("refused")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT,
                assertThrows(AiIntegrationException.class, () -> client.fetch(request)).getStatus());
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(AiIntegrationException.class, () -> client.fetch(request)).getStatus());
        server.verify();
    }

    @Test
    void rejectsMissingResponseFields() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var error = assertThrows(AiIntegrationException.class, () -> client.fetch(request));
        assertEquals("AiInvalidResponse", error.getCode());
        server.verify();
    }

    @Test
    void rejectsOutOfRangeScore() {
        server.expect(requestTo("https://ai.invalid/alternative"))
                .andRespond(withSuccess("""
                        {"triggered":true,"targetQuietIndex":35,"alternatives":[
                          {"poiId":"126078","quietIndex":80,"distanceKm":1,
                           "score":2,"recommendReason":"Quieter"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        assertEquals("AiInvalidResponse",
                assertThrows(AiIntegrationException.class, () -> client.fetch(request)).getCode());
        server.verify();
    }

    @Test
    void missingConfigurationFailsWithoutCallingAi() {
        var unconfigured = new AiAlternativeClient(RestClient.create(),
                new AiProperties(null, null, "real", 3000, 30000));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(AiIntegrationException.class, () -> unconfigured.fetch(request)).getStatus());
    }
}
