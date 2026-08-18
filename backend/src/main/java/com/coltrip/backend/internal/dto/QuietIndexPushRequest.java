package com.coltrip.backend.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record QuietIndexPushRequest(
        @NotBlank String tourApiContentId,
        @NotNull @Min(0) @Max(100) Integer quietScore,
        @NotNull LocalDateTime calculatedAt,
        String rawMetrics
) {
}
