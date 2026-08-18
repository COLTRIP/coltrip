package com.coltrip.backend.visit.dto;

import com.coltrip.backend.domain.visit.Visit;
import java.time.LocalDateTime;

public record VisitStartResponse(
        Long visitId,
        String status,
        LocalDateTime startedAt
) {
    public static VisitStartResponse from(Visit visit) {
        return new VisitStartResponse(visit.getId(), visit.getStatus().name(), visit.getStartedAt());
    }
}
