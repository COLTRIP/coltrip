package com.coltrip.backend.review.dto;

import com.coltrip.backend.domain.review.QuietFeedback;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull QuietFeedback quietFeedback,
        @Size(max = 300) String content
) {
}
