package com.coltrip.backend.forecast;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.coltrip.backend.domain.forecast.*;
import com.coltrip.backend.domain.spot.*;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForecastQueryServiceTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 30);
    private QuietForecastRepository forecasts;
    private TouristSpotRepository spots;
    private SpotLikeRepository likes;
    private ForecastQueryService service;

    @BeforeEach void setup() {
        forecasts = mock(QuietForecastRepository.class);
        spots = mock(TouristSpotRepository.class);
        likes = mock(SpotLikeRepository.class);
        service = new ForecastQueryService(forecasts, spots, likes, new ForecastPolicy(), "coltrip-ai",
                Clock.fixed(now.atZone(ForecastPolicy.ZONE).toInstant(), ForecastPolicy.ZONE));
    }

    @Test void emptyForecastDoesNotReadCurrentScore() {
        var response = service.recommend(null, now.toLocalDate(), 13, null, null, 20);
        assertTrue(response.spots().isEmpty());
        assertFalse(response.message().isBlank());
        verifyNoInteractions(spots, likes);
    }

    @Test void sortsByForecastScoreThenIdAndLimits() {
        var lower = forecast(1L, "70.25", "35.01");
        var far = forecast(2L, "100", "36.0");
        var tiedFarther = forecast(3L, "80.75", "35.02");
        var tiedNearer = forecast(4L, "80.75", "35.01");
        when(forecasts.findCandidates(any(), anyString(), any(), eq(Category.PARK), eq(Mode.NATURAL)))
                .thenReturn(List.of(lower, far, tiedFarther, tiedNearer));
        when(likes.findLikedSpotIds(eq(7L), anyCollection())).thenReturn(Set.of(3L));
        var result = service.recommend(7L, now.toLocalDate(), 13, Category.PARK, Mode.NATURAL, 2);
        assertEquals(List.of(2L, 3L), result.spots().stream().map(i -> i.spot().id()).toList());
        assertEquals(bd("100"), result.spots().getFirst().forecast().quietIndex());
        assertTrue(result.spots().get(1).spot().isLiked());
        verify(forecasts).findCandidates(eq(now.withMinute(0).withHour(13)), eq("coltrip-ai"), eq(now),
                eq(Category.PARK), eq(Mode.NATURAL));
    }

    @Test void limitValidationRejectsOutOfRange() {
        assertThrows(InvalidForecastRequestException.class,
                () -> service.recommend(null, now.toLocalDate(), 13, null, null, 51));
    }

    @Test void timelineHas24SlotsAndNeverFillsMissingValues() {
        when(spots.existsById(1L)).thenReturn(true);
        var value = forecast(1L, "80", "35");
        when(forecasts.findTimeline(anyLong(), any(), any(), anyString(), any())).thenReturn(List.of(value));
        var result = service.timeline(1L, now.toLocalDate(), 13);
        assertEquals(24, result.timeline().size());
        assertEquals(bd("80"), result.timeline().getFirst().quietIndex());
        assertNull(result.timeline().get(1).quietIndex());
        assertNull(result.timeline().get(1).generatedAt());
        assertEquals("FORECAST", result.timeline().get(1).type());
    }

    @Test void missingSpotAndHorizonOverflowAreRejected() {
        assertThrows(SpotNotFoundException.class, () -> service.timeline(999L, now.toLocalDate(), 13));
        assertThrows(InvalidForecastRequestException.class, () -> service.timeline(1L, now.plusDays(7).toLocalDate(), 12));
    }

    private QuietForecast forecast(Long id, String score, String latitude) {
        TouristSpot spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(id);
        when(spot.getLatitude()).thenReturn(bd(latitude));
        when(spot.getLongitude()).thenReturn(bd("129"));
        when(spot.getCategory()).thenReturn(Category.PARK);
        when(spot.getModes()).thenReturn(List.of(Mode.NATURAL));
        QuietForecast f = new QuietForecast(spot, now.withMinute(0).withHour(13), now.minusHours(1), "coltrip-ai");
        f.correct(bd(score), now.plusHours(2), now, "test-model");
        return f;
    }
    private BigDecimal bd(String value) { return new BigDecimal(value); }
}
