package com.coltrip.backend.review.dto;

import com.coltrip.backend.domain.review.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long spotId,
        Long userId,
        String nickname,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getSpot().getId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
