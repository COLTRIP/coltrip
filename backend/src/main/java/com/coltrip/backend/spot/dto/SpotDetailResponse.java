package com.coltrip.backend.spot.dto;

import com.coltrip.backend.domain.spot.QuietLevel;
import com.coltrip.backend.domain.spot.TouristSpot;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "visitRadiusMeters 판정 기준의 점형/면적형 구분", allowableValues = {"POINT", "AREA"})
        String spotAreaType,
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
                spot.getCategory().getAreaType().name(),
                isLiked
        );
    }
}
