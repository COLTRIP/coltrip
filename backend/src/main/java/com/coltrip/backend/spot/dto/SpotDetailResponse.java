package com.coltrip.backend.spot.dto;

import com.coltrip.backend.domain.spot.QuietLevel;
import com.coltrip.backend.domain.spot.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SpotDetailResponse(
        Long id,
        String name,
        String address,
        String category,
        List<String> modes,
        String description,
        String imageUrl,
        String recommendReason,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer quietScore,
        String quietLevel,
        LocalDateTime quietScoreUpdatedAt,
        Integer visitRadiusMeters,
        boolean isLiked
) {
    public static SpotDetailResponse from(TouristSpot spot, boolean isLiked) {
        QuietLevel quietLevel = spot.getQuietLevel();
        return new SpotDetailResponse(
                spot.getId(),
                spot.getName(),
                spot.getAddress(),
                spot.getCategory().name(),
                spot.getModes().stream().map(Enum::name).toList(),
                spot.getDescription(),
                spot.getImageUrl(),
                spot.getRecommendReason(),
                spot.getLatitude(),
                spot.getLongitude(),
                spot.getCurrentQuietScore(),
                quietLevel == null ? null : quietLevel.name(),
                spot.getQuietScoreUpdatedAt(),
                spot.getCategory().getVisitRadiusMeters(),
                isLiked
        );
    }
}
