package com.coltrip.backend.visit.dto;

import com.coltrip.backend.domain.visit.Visit;

public record VisitCancelResponse(
        Long visitId,
        String status
) {
    public static VisitCancelResponse from(Visit visit) {
        return new VisitCancelResponse(visit.getId(), visit.getStatus().name());
    }
}
