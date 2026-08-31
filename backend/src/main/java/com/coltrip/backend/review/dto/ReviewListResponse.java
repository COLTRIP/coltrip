package com.coltrip.backend.review.dto;

import com.coltrip.backend.domain.review.Review;
import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> reviews
) {
    public static ReviewListResponse from(List<Review> reviews) {
        return new ReviewListResponse(reviews.stream().map(ReviewResponse::from).toList());
    }
}
