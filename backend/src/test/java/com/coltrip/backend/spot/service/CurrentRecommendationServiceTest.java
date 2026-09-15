package com.coltrip.backend.spot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.forecast.ForecastPolicy;
import com.coltrip.backend.forecast.InvalidForecastRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentRecommendationServiceTest {
    @Test void sortsByQuietScoreDescThenIdAndAnonymousSkipsLikes() {
        var spots = mock(TouristSpotRepository.class);
        var likes = mock(SpotLikeRepository.class);
        var service = new CurrentRecommendationService(spots, likes, new ForecastPolicy());
        var lower = spot(1L, 50);
        var higher = spot(2L, 90);
        when(spots.findByFilters(isNull(), isNull())).thenReturn(List.of(lower, higher));
        var result = service.recommend(null, null, null, 20);
        assertEquals(2, result.spots().size());
        assertEquals(2L, result.spots().getFirst().spot().id());
        assertEquals(1L, result.spots().get(1).spot().id());
        verifyNoInteractions(likes);
    }

    @Test void limitValidationRejectsOutOfRange() {
        var spots = mock(TouristSpotRepository.class);
        var likes = mock(SpotLikeRepository.class);
        var service = new CurrentRecommendationService(spots, likes, new ForecastPolicy());
        assertThrows(InvalidForecastRequestException.class, () -> service.recommend(null, null, null, 51));
    }

    private TouristSpot spot(Long id, Integer quietScore) {
        var spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(id);
        when(spot.getLatitude()).thenReturn(new BigDecimal("35"));
        when(spot.getLongitude()).thenReturn(new BigDecimal("129"));
        when(spot.getCategory()).thenReturn(com.coltrip.backend.domain.spot.Category.PARK);
        when(spot.getModes()).thenReturn(List.of());
        when(spot.getCurrentQuietScore()).thenReturn(quietScore);
        return spot;
    }
}
