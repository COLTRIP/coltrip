package com.coltrip.backend.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public record QuietIndexPushRequest(
        @NotBlank String tourApiContentId,
        @NotNull @Min(0) @Max(100) Integer quietScore,
        @Schema(description = "한국 시간(Asia/Seoul) 관측 시각. 서버 검증 시각보다 미래인 값은 거부하며 허용 오차는 0.")
        @NotNull LocalDateTime calculatedAt,
        String rawMetrics
) {
}
