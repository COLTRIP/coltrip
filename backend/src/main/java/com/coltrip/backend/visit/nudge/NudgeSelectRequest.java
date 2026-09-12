package com.coltrip.backend.visit.nudge;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record NudgeSelectRequest(
        @NotNull Long spotId,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal startLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal startLongitude) {
}
