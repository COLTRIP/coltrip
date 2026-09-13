package com.coltrip.backend.forecast;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.domain.forecast.QuietForecastRepository;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.spot.controller.SpotController;
import com.coltrip.backend.spot.service.SpotService;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ForecastControllerTest {
    private MockMvc mvc;

    @BeforeEach void setup() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-13T03:00:00Z"), ForecastPolicy.ZONE);
        var forecasts = mock(QuietForecastRepository.class);
        var spots = mock(TouristSpotRepository.class);
        var query = new ForecastQueryService(forecasts, spots, mock(SpotLikeRepository.class), new ForecastPolicy(), "coltrip-ai", clock);
        var ingest = new ForecastIngestService(spots, forecasts, new InternalApiProperties("test-key"), new ForecastPolicy(), "coltrip-ai", clock);
        mvc = MockMvcBuilders.standaloneSetup(new ForecastController(query, ingest), new SpotController(mock(SpotService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test void recommendationsRouteDoesNotBecomeSpotId() throws Exception {
        mvc.perform(get("/api/spots/recommendations").param("date", "2026-09-13").param("hour", "13"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots").isEmpty())
                .andExpect(jsonPath("$.targetAt").value("2026-09-13T13:00:00+09:00"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void invalidDateHourAndEnumAre400() throws Exception {
        mvc.perform(get("/api/spots/recommendations").param("date", "not-a-date").param("hour", "13"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/spots/recommendations").param("date", "2026-09-13").param("hour", "24"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/spots/recommendations").param("date", "2026-09-13").param("hour", "13").param("mode", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test void internalPushRequiresKey() throws Exception {
        mvc.perform(post("/api/internal/quiet-index/forecasts").contentType("application/json").content(body("80")))
                .andExpect(status().isUnauthorized());
    }

    @Test void scoreRangeIsValidatedBeforePersistence() throws Exception {
        mvc.perform(post("/api/internal/quiet-index/forecasts").header("X-Internal-Api-Key", "test-key")
                        .contentType("application/json").content(body("101")))
                .andExpect(status().isBadRequest());
    }

    private String body(String score) {
        return """
                {"source":"coltrip-ai","modelVersion":"test","generatedAt":"2026-09-13T11:00:00+09:00",
                  "forecasts":[{"tourApiContentId":"123","targetAt":"2026-09-13T13:00:00+09:00",
                    "quietIndex":%s,"validUntil":"2026-09-13T14:00:00+09:00"}]}
                """.formatted(score);
    }
}
