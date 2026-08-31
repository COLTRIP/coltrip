package com.coltrip.backend.spot.dto;

import com.coltrip.backend.domain.spot.QuietLevel;
import com.coltrip.backend.domain.spot.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SpotSummaryResponse(
        Long id,
        String name,
        String address,
        String category,
        List<String> modes,
        String imageUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer quietScore,
        String quietLevel,
        LocalDateTime quietScoreUpdatedAt
) {
    public static SpotSummaryResponse from(TouristSpot spot) {
        QuietLevel quietLevel = spot.getQuietLevel();
        return new SpotSummaryResponse(
                spot.getId(),
                spot.getName(),
                spot.getAddress(),
                spot.getCategory().name(),
                spot.getModes().stream().map(Enum::name).toList(),
                spot.getImageUrl(),
                spot.getLatitude(),
                spot.getLongitude(),
                spot.getCurrentQuietScore(),
                quietLevel == null ? null : quietLevel.name(),
                spot.getQuietScoreUpdatedAt()
        );
    }
}
