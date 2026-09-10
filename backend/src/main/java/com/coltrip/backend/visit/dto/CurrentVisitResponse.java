package com.coltrip.backend.visit.dto;

import com.coltrip.backend.domain.spot.QuietLevel;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.visit.Visit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrentVisitResponse(
        CurrentVisit visit
) {
    public static CurrentVisitResponse from(Visit visit) {
        return new CurrentVisitResponse(CurrentVisit.from(visit));
    }

    public static CurrentVisitResponse empty() {
        return new CurrentVisitResponse(null);
    }

    public record CurrentVisit(
            Long visitId,
            Long spotId,
            String spotName,
            String spotAddress,
            String category,
            String imageUrl,
            BigDecimal latitude,
            BigDecimal longitude,
            String status,
            LocalDateTime startedAt,
            Integer startQuietScore,
            Integer currentQuietScore,
            String currentQuietLevel,
            Integer visitRadiusMeters
    ) {
        private static CurrentVisit from(Visit visit) {
            TouristSpot spot = visit.getSpot();
            QuietLevel quietLevel = spot.getQuietLevel();

            return new CurrentVisit(
                    visit.getId(),
                    spot.getId(),
                    spot.getName(),
                    spot.getAddress(),
                    spot.getCategory().name(),
                    spot.getImageUrl(),
                    spot.getLatitude(),
                    spot.getLongitude(),
                    visit.getStatus().name(),
                    visit.getStartedAt(),
                    visit.getStartQuietScore(),
                    spot.getCurrentQuietScore(),
                    quietLevel == null ? null : quietLevel.name(),
                    spot.getCategory().getVisitRadiusMeters()
            );
        }
    }
}