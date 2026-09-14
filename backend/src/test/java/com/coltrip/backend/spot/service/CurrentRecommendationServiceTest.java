package com.coltrip.backend.spot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.forecast.ForecastPolicy;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentRecommendationServiceTest {
    @Test void radiusUsesUnroundedDistanceAndAnonymousSkipsLikes() {
        var spots = mock(TouristSpotRepository.class);
        var likes = mock(SpotLikeRepository.class);
        var service = new CurrentRecommendationService(spots, likes, new ForecastPolicy());
        double boundary = 35 + Math.toDegrees(15000.0 / 6371000);
        var inside = spot(1L, boundary - .0000001);
        var outside = spot(2L, boundary + .0000001);
        when(spots.findInBounds(any(), any(), any(), any(), isNull(), isNull())).thenReturn(List.of(outside, inside));
        var result = service.recommend(null, new BigDecimal("35"), new BigDecimal("129"), 15000, null, null, 20);
        assertEquals(1, result.spots().size());
        assertEquals(1L, result.spots().getFirst().spot().id());
        assertEquals(15000, result.spots().getFirst().distanceMeters());
        verifyNoInteractions(likes);
    }

    private TouristSpot spot(Long id, double latitude) {
        var spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(id);
        when(spot.getLatitude()).thenReturn(BigDecimal.valueOf(latitude));
        when(spot.getLongitude()).thenReturn(new BigDecimal("129"));
        when(spot.getCategory()).thenReturn(com.coltrip.backend.domain.spot.Category.PARK);
        when(spot.getModes()).thenReturn(List.of());
        return spot;
    }
}
