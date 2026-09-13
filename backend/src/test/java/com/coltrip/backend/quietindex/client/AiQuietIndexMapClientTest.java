package com.coltrip.backend.quietindex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiQuietIndexMapClientTest {

    private MockRestServiceServer server;
    private AiQuietIndexMapClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiQuietIndexMapClient(builder.build(),
                new AiProperties("https://ai.invalid", "test-key", "real", 3000, 30000));
    }

    @Test
    void fetchesWithAuthHeaderAndCamelCaseQueryParams() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=14&isWeekend=false"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess("""
                        [{"poiId":"126081","name":"해운대해수욕장","population":13014,"quietIndex":85.4}]
                        """, MediaType.APPLICATION_JSON));

        List<AiQuietIndexMapClient.Item> items = client.fetchMap(14, false);

        assertEquals(1, items.size());
        assertEquals("126081", items.getFirst().poiId());
        assertEquals(85.4, items.getFirst().quietIndex());
        server.verify();
    }

    @Test
    void emptyBodyReturnsEmptyList() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=0&isWeekend=true"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchMap(0, true).isEmpty());
        server.verify();
    }

    @Test
    void upstreamFailureBecomesBadGateway() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=9&isWeekend=false"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var error = assertThrows(AiIntegrationException.class, () -> client.fetchMap(9, false));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        server.verify();
    }

    @Test
    void missingConfigurationFailsWithoutCallingAi() {
        var unconfigured = new AiQuietIndexMapClient(RestClient.create(),
                new AiProperties(null, null, "real", 3000, 30000));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(AiIntegrationException.class, () -> unconfigured.fetchMap(9, false)).getStatus());
    }
}
