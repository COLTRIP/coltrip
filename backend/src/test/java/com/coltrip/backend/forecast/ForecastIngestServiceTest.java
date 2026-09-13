package com.coltrip.backend.forecast;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.forecast.*;
import com.coltrip.backend.domain.spot.*;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ForecastIngestServiceTest {
    private QuietForecastRepository forecasts;
    private TouristSpotRepository spots;
    private ForecastIngestService service;
    private final OffsetDateTime now = OffsetDateTime.parse("2026-09-13T12:00:00+09:00");

    @BeforeEach void setup() {
        forecasts = mock(QuietForecastRepository.class);
        spots = mock(TouristSpotRepository.class);
        service = new ForecastIngestService(spots, forecasts, new InternalApiProperties("test-key"),
                new ForecastPolicy(), "coltrip-ai", Clock.fixed(now.toInstant(), ForecastPolicy.ZONE));
    }

    @Test void rejectsWrongOrMissingKeyBeforeDatabaseAccess() {
        assertThrows(InvalidInternalApiKeyException.class, () -> service.receive(null, request()));
        assertThrows(InvalidInternalApiKeyException.class, () -> service.receive("wrong", request()));
        verifyNoInteractions(spots, forecasts);
    }

    @Test void storesSourceTimesAndRoundsWithoutChangingObservedCache() {
        TouristSpot spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(1L);
        when(spots.findByTourApiContentIdForUpdate("123")).thenReturn(Optional.of(spot));
        var response = service.receive("test-key", request());
        assertEquals(1, response.created());
        var captor = ArgumentCaptor.forClass(QuietForecast.class);
        verify(forecasts).save(captor.capture());
        assertEquals(new BigDecimal("80.76"), captor.getValue().getQuietIndex());
        assertEquals(now.minusHours(1).toLocalDateTime(), captor.getValue().getGeneratedAt());
        assertEquals(now.toLocalDateTime(), captor.getValue().getReceivedAt());
        verify(spot, never()).updateQuietScoreIfNewer(anyInt(), any());
    }

    @Test void identicalGenerationUpdatesExistingVersion() {
        TouristSpot spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(1L);
        when(spots.findByTourApiContentIdForUpdate("123")).thenReturn(Optional.of(spot));
        QuietForecast previous = new QuietForecast(spot, now.plusHours(1).toLocalDateTime(), now.minusHours(1).toLocalDateTime(), "coltrip-ai");
        when(forecasts.findBySpot_IdAndTargetAtAndSourceAndGeneratedAt(any(), any(), any(), any())).thenReturn(Optional.of(previous));
        var result = service.receive("test-key", request());
        assertEquals(0, result.created());
        assertEquals(1, result.updated());
        verify(forecasts).save(previous);
    }

    @Test void rejectsFutureGenerationAndDuplicateTargets() {
        var valid = request();
        assertThrows(InvalidForecastRequestException.class, () -> service.receive("test-key",
                new ForecastPushRequest("coltrip-ai", "test-model", now.plusSeconds(1), valid.forecasts())));
        assertThrows(InvalidForecastRequestException.class, () -> service.receive("test-key",
                new ForecastPushRequest("coltrip-ai", "test-model", now.minusHours(1),
                        List.of(valid.forecasts().getFirst(), valid.forecasts().getFirst()))));
        verifyNoInteractions(spots, forecasts);
    }

    @Test void rejectsNonHourlyTargetAndInvalidExpiry() {
        var item = request().forecasts().getFirst();
        assertThrows(InvalidForecastRequestException.class, () -> service.receive("test-key",
                withItem(new ForecastPushRequest.Item("123", item.targetAt().plusMinutes(1), item.quietIndex(), item.validUntil()))));
        assertThrows(InvalidForecastRequestException.class, () -> service.receive("test-key",
                withItem(new ForecastPushRequest.Item("123", item.targetAt(), item.quietIndex(), now.minusHours(2)))));
    }

    @Test void unknownSpotIsNotSilentlyCreated() {
        assertThrows(SpotNotFoundException.class, () -> service.receive("test-key", request()));
        verify(forecasts, never()).save(any());
    }

    private ForecastPushRequest request() {
        return withItem(new ForecastPushRequest.Item("123", now.plusHours(1), new BigDecimal("80.755"), now.plusHours(2)));
    }
    private ForecastPushRequest withItem(ForecastPushRequest.Item item) {
        return new ForecastPushRequest("coltrip-ai", "test-model", now.minusHours(1), List.of(item));
    }
}
