package com.coltrip.backend.visit.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VisitCompleteRequest(
        @NotNull BigDecimal arrivedLatitude,
        @NotNull BigDecimal arrivedLongitude
) {
}
