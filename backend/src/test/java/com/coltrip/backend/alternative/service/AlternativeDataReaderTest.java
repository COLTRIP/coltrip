package com.coltrip.backend.alternative.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlternativeDataReaderTest {
    private TouristSpotRepository spots;
    private VisitRepository visits;
    private SpotLikeRepository likes;

    @BeforeEach
    void setUp() {
        spots = mock(TouristSpotRepository.class);
        visits = mock(VisitRepository.class);
        likes = mock(SpotLikeRepository.class);
    }

    @Test
    void mapsKnownMockSpotWithoutQueryingAnonymousVisits() {
        spot(1L, "SEED-008");
        var target = reader("mock").loadTarget(null, 1L);
        assertEquals("POI001", target.aiPoiId());
        assertNull(target.baselineQuietIndex());
        verifyNoInteractions(visits);
    }

    @Test
    void realModeUsesContentIdAndOnlyMatchingUsersStartedVisit() {
        TouristSpot origin = spot(1L, "123456");
        TouristSpot other = mock(TouristSpot.class);
        when(other.getId()).thenReturn(2L);
        Visit wrongSpot = mock(Visit.class);
        when(wrongSpot.getSpot()).thenReturn(other);
        Visit matching = mock(Visit.class);
        when(matching.getSpot()).thenReturn(origin);
        when(matching.getStartQuietScore()).thenReturn(65);
        when(visits.findByUserIdAndStatusWithSpot(7L, VisitStatus.STARTED))
                .thenReturn(List.of(wrongSpot, matching));
        var target = reader("real").loadTarget(7L, 1L);
        assertEquals("123456", target.aiPoiId());
        assertEquals(65.0, target.baselineQuietIndex());
        verify(visits).findByUserIdAndStatusWithSpot(7L, VisitStatus.STARTED);
        verifyNoMoreInteractions(visits);
    }

    @Test
    void missingVisitScoreRemainsNull() {
        TouristSpot origin = spot(1L, "123456");
        Visit visit = mock(Visit.class);
        when(visit.getSpot()).thenReturn(origin);
        when(visit.getStartQuietScore()).thenReturn(null);
        when(visits.findByUserIdAndStatusWithSpot(7L, VisitStatus.STARTED)).thenReturn(List.of(visit));
        assertNull(reader("real").loadTarget(7L, 1L).baselineQuietIndex());
    }

    @Test
    void unmappedMockAndSeedInRealModeAreRejected() {
        spot(1L, "SEED-003");
        assertEquals("AiPoiNotMapped", assertThrows(AiIntegrationException.class,
                () -> reader("mock").loadTarget(null, 1L)).getCode());
        assertThrows(AiIntegrationException.class, () -> reader("real").loadTarget(null, 1L));
    }

    @Test
    void unknownMockCandidatesDoNotQueryDatabase() {
        assertTrue(reader("mock").loadPlaces(null, List.of("POI999")).isEmpty());
        verifyNoInteractions(spots, visits, likes);
    }

    private TouristSpot spot(Long id, String contentId) {
        TouristSpot spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(id);
        when(spot.getTourApiContentId()).thenReturn(contentId);
        when(spots.findById(id)).thenReturn(Optional.of(spot));
        return spot;
    }

    private AlternativeDataReader reader(String mode) {
        return new AlternativeDataReader(spots, visits, likes,
                new AiProperties("https://ai.invalid", "test", mode, 3000, 30000));
    }
}
