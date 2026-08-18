package com.coltrip.backend.visit.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VisitStartRequest(
        @NotNull Long spotId,
        @NotNull BigDecimal startLatitude,
        @NotNull BigDecimal startLongitude
) {
}
