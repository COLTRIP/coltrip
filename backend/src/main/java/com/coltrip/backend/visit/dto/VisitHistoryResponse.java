package com.coltrip.backend.visit.dto;

import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.visit.Visit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record VisitHistoryResponse(
        List<VisitHistoryItem> visits
) {
    public static VisitHistoryResponse from(List<Visit> visits, Map<Long, Long> reviewIdByVisitId) {
        return new VisitHistoryResponse(visits.stream()
                .map(visit -> VisitHistoryItem.from(visit, reviewIdByVisitId.get(visit.getId())))
                .toList());
    }

    public static VisitHistoryResponse empty() {
        return new VisitHistoryResponse(List.of());
    }

    public record VisitHistoryItem(
            Long visitId,
            Long spotId,
            String spotName,
            String spotAddress,
            String category,
            String imageUrl,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            Long reviewId
    ) {
        private static VisitHistoryItem from(Visit visit, Long reviewId) {
            TouristSpot spot = visit.getSpot();
            return new VisitHistoryItem(
                    visit.getId(),
                    spot.getId(),
                    spot.getName(),
                    spot.getAddress(),
                    spot.getCategory().name(),
                    spot.getImageUrl(),
                    visit.getStartedAt(),
                    visit.getCompletedAt(),
                    reviewId
            );
        }
    }
}
