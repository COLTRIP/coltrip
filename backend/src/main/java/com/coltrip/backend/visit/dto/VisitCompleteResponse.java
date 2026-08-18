package com.coltrip.backend.visit.dto;

import com.coltrip.backend.domain.visit.Visit;
import java.time.LocalDateTime;

public record VisitCompleteResponse(
        Long visitId,
        String status,
        LocalDateTime completedAt
) {
    public static VisitCompleteResponse from(Visit visit) {
        return new VisitCompleteResponse(visit.getId(), visit.getStatus().name(), visit.getCompletedAt());
    }
}
