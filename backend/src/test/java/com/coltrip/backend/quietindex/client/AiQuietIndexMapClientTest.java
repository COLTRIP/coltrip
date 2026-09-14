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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fetchesWithAuthHeaderAndSnakeCaseWeekendQuery(boolean isWeekend) {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=14&is_weekend=" + isWeekend))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess("""
                        [{"poiId":"126081","name":"해운대해수욕장","lat":35.1587,"lng":129.1604,"quietIndex":85.4}]
                        """, MediaType.APPLICATION_JSON));

        List<AiQuietIndexMapClient.Item> items = client.fetchMap(14, isWeekend);

        assertEquals(1, items.size());
        assertEquals("126081", items.getFirst().poiId());
        assertEquals(85.4, items.getFirst().quietIndex());
        server.verify();
    }

    @Test
    void emptyArrayReturnsEmptyList() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=0&is_weekend=true"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchMap(0, true).isEmpty());
        server.verify();
    }

    @Test
    void nullEntryIsPreservedForPerItemValidation() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=9&is_weekend=false"))
                .andRespond(withSuccess("[null,{\"poiId\":\"126081\",\"quietIndex\":50}]", MediaType.APPLICATION_JSON));
        var items = client.fetchMap(9, false);
        assertEquals(2, items.size());
        org.junit.jupiter.api.Assertions.assertNull(items.getFirst());
        assertEquals(50.0, items.get(1).quietIndex());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{broken", "{\"unexpected\":true}"})
    void absentOrMalformedBodyFailsWholeFetch(String body) {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=9&is_weekend=false"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(AiIntegrationException.class, () -> client.fetchMap(9, false)).getStatus());
        server.verify();
    }

    @Test
    void upstreamFailureBecomesBadGateway() {
        server.expect(requestTo("https://ai.invalid/quiet-index/map?hour=9&is_weekend=false"))
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
