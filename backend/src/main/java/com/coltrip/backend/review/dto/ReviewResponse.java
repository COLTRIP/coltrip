package com.coltrip.backend.review.dto;

import com.coltrip.backend.domain.review.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long spotId,
        Long userId,
        String nickname,
        String quietFeedback,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getSpot().getId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getQuietFeedback().name(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
