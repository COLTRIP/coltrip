package com.coltrip.backend.internal.spot;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.SpotModeRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpotImportControllerTest {
    private MockMvc mvc;
    private TouristSpotRepository spots;

    @BeforeEach void setup() {
        spots = mock(TouristSpotRepository.class);
        var service = new SpotImportService(spots, mock(SpotModeRepository.class), new InternalApiProperties("test-key"),
                Clock.fixed(Instant.parse("2026-09-13T03:00:00Z"), ZoneId.of("Asia/Seoul")));
        mvc = MockMvcBuilders.standaloneSetup(new SpotImportController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void missingAndWrongKeysAreRejected() throws Exception {
        mvc.perform(post("/api/internal/spots").contentType("application/json").content(body()))
                .andExpect(status().isUnauthorized());
        request(body(), "wrong", 401);
        verifyNoInteractions(spots);
    }

    @Test void requiredFieldsAndEnumsAreValidated() throws Exception {
        request(body().replace("\"name\":\"Place\"", "\"name\":\"\""), "test-key", 400);
        request(body().replace("\"PARK\"", "\"UNKNOWN\""), "test-key", 400);
        request(body().replace("\"modes\":[\"WALK\"]", "\"modes\":null"), "test-key", 400);
        request(body().replace("\"WALK\"", "\"COZY\""), "test-key", 400);
        request(body().replace("\"123\"", "\"POI001\""), "test-key", 400);
        verifyNoInteractions(spots);
    }

    @Test void coordinatesAndUnsafeImageUrlsAreRejected() throws Exception {
        request(body().replace("\"latitude\":35", "\"latitude\":91"), "test-key", 400);
        request(body().replace("\"latitude\":35", "\"latitude\":35.12345678"), "test-key", 400);
        request(body().replace("\"imageUrl\":null", "\"imageUrl\":\"javascript:alert(1)\""), "test-key", 400);
        request(body().replace("\"imageUrl\":null", "\"imageUrl\":\"https://user:password@example.com/a.jpg\""), "test-key", 400);
        verifyNoInteractions(spots);
    }

    @Test void futureSourceTimeAndDuplicateIdsAreRejectedBeforeWriting() throws Exception {
        request(body().replace("2026-09-13T11:00:00+09:00", "2026-09-13T13:00:00+09:00"), "test-key", 400);
        String item = body().substring(body().indexOf('[') + 1, body().lastIndexOf(']')).trim();
        request("{\"spots\":[" + item + "," + item + "]}", "test-key", 400);
        verifyNoInteractions(spots);
    }

    @Test void missingTimestampAndEmptyBatchAreRejected() throws Exception {
        request(body().replace("\"sourceUpdatedAt\":\"2026-09-13T11:00:00+09:00\"", "\"sourceUpdatedAt\":null"), "test-key", 400);
        request("{\"spots\":[]}", "test-key", 400);
        verifyNoInteractions(spots);
    }

    private void request(String body, String key, int status) throws Exception {
        mvc.perform(post("/api/internal/spots").header("X-Internal-Api-Key", key)
                .contentType("application/json").content(body)).andExpect(status().is(status));
    }

    private String body() {
        return """
                {"spots":[{"tourApiContentId":"123","name":"Place","address":"Busan",
                  "latitude":35,"longitude":129,"category":"PARK","description":null,
                  "imageUrl":null,"recommendReason":null,"modes":["WALK"],
                  "sourceUpdatedAt":"2026-09-13T11:00:00+09:00"}]}
                """;
    }
}
