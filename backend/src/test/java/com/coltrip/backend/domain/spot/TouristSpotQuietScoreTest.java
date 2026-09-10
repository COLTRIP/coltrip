package com.coltrip.backend.domain.spot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TouristSpotQuietScoreTest {

    private TouristSpot newSpot() {
        return TouristSpot.builder()
                .tourApiContentId("TEST-SPOT")
                .name("테스트 장소")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.0))
                .longitude(BigDecimal.valueOf(129.0))
                .category(Category.CAFE)
                .build();
    }

    @Test
    void firstUpdateAlwaysApplies() {
        TouristSpot spot = newSpot();
        LocalDateTime at = LocalDateTime.of(2026, 9, 10, 10, 0);

        spot.updateQuietScoreIfNewer(80, at);

        assertEquals(80, spot.getCurrentQuietScore());
        assertEquals(at, spot.getQuietScoreUpdatedAt());
    }

    @Test
    void olderCalculatedAtIsIgnored() {
        TouristSpot spot = newSpot();
        spot.updateQuietScoreIfNewer(80, LocalDateTime.of(2026, 9, 10, 10, 0));

        spot.updateQuietScoreIfNewer(30, LocalDateTime.of(2026, 9, 10, 9, 0));

        assertEquals(80, spot.getCurrentQuietScore());
        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 0), spot.getQuietScoreUpdatedAt());
    }

    @Test
    void sameCalculatedAtIsTreatedAsCorrectionAndApplies() {
        TouristSpot spot = newSpot();
        LocalDateTime at = LocalDateTime.of(2026, 9, 10, 10, 0);
        spot.updateQuietScoreIfNewer(80, at);

        spot.updateQuietScoreIfNewer(55, at);

        assertEquals(55, spot.getCurrentQuietScore());
        assertEquals(at, spot.getQuietScoreUpdatedAt());
    }

    @Test
    void newerCalculatedAtApplies() {
        TouristSpot spot = newSpot();
        spot.updateQuietScoreIfNewer(80, LocalDateTime.of(2026, 9, 10, 10, 0));

        spot.updateQuietScoreIfNewer(20, LocalDateTime.of(2026, 9, 10, 11, 0));

        assertEquals(20, spot.getCurrentQuietScore());
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 0), spot.getQuietScoreUpdatedAt());
    }
}
