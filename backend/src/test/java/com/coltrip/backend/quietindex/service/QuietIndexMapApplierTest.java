package com.coltrip.backend.quietindex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuietIndexMapApplierTest {

    @Mock
    private TouristSpotRepository spots;

    @Mock
    private QuietIndexRepository quietIndexRepository;

    @Test
    void roundsDecimalScoreAndUpdatesCacheWhenSpotIsKnown() {
        QuietIndexMapApplier applier = new QuietIndexMapApplier(spots, quietIndexRepository);
        TouristSpot spot = newSpot();
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 13, 14, 5);
        when(spots.findByTourApiContentIdForUpdate("126081")).thenReturn(Optional.of(spot));
        when(quietIndexRepository.findBySpot_IdAndCalculatedAt(spot.getId(), calculatedAt))
                .thenReturn(Optional.empty());

        boolean applied = applier.apply("126081", 85.6, calculatedAt);

        assertTrue(applied);
        assertEquals(86, spot.getCurrentQuietScore());
        verify(quietIndexRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsInclusiveScoreBounds() {
        var applier = new QuietIndexMapApplier(spots, quietIndexRepository);
        TouristSpot spot = newSpot();
        when(spots.findByTourApiContentIdForUpdate("126081")).thenReturn(Optional.of(spot));
        LocalDateTime now = LocalDateTime.now();
        assertTrue(applier.apply("126081", 0, now));
        assertEquals(0, spot.getCurrentQuietScore());
        assertTrue(applier.apply("126081", 100, now.plusSeconds(1)));
        assertEquals(100, spot.getCurrentQuietScore());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 100.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidScoreNeverTouchesDatabase(double score) {
        var applier = new QuietIndexMapApplier(spots, quietIndexRepository);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> applier.apply("126081", score, LocalDateTime.now()));
        org.mockito.Mockito.verifyNoInteractions(spots, quietIndexRepository);
    }

    @Test
    void unknownPoiIdIsSkippedWithoutTouchingHistory() {
        QuietIndexMapApplier applier = new QuietIndexMapApplier(spots, quietIndexRepository);
        when(spots.findByTourApiContentIdForUpdate("999999")).thenReturn(Optional.empty());

        boolean applied = applier.apply("999999", 50.0, LocalDateTime.now());

        assertFalse(applied);
        verify(quietIndexRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private TouristSpot newSpot() {
        return TouristSpot.builder()
                .tourApiContentId("126081")
                .name("해운대해수욕장")
                .address("부산 해운대구")
                .latitude(BigDecimal.valueOf(35.0))
                .longitude(BigDecimal.valueOf(129.0))
                .category(Category.BEACH)
                .build();
    }
}
