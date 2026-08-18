package com.coltrip.backend.visit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record VisitCompleteRequest(
        @NotNull BigDecimal arrivedLatitude,
        @NotNull BigDecimal arrivedLongitude,
        @NotNull @PositiveOrZero Long stayDurationSeconds
) {
}
