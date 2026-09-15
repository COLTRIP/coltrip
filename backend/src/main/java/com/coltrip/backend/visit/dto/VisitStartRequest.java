package com.coltrip.backend.visit.dto;

import jakarta.validation.constraints.NotNull;

public record VisitStartRequest(
        @NotNull Long spotId
) {
}
