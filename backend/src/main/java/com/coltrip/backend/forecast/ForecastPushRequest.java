package com.coltrip.backend.forecast;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ForecastPushRequest(
        @NotBlank @Size(max = 64) String source,
        @NotBlank @Size(max = 100) String modelVersion,
        @NotNull OffsetDateTime generatedAt,
        @NotNull @Size(min = 1, max = 1000) List<@NotNull @Valid Item> forecasts) {

    public record Item(
            @NotBlank @Pattern(regexp = "[0-9]{1,50}") String tourApiContentId,
            @NotNull OffsetDateTime targetAt,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal quietIndex,
            @NotNull OffsetDateTime validUntil) { }
}
