package com.coltrip.backend.review.controller;

import com.coltrip.backend.review.dto.ReviewCreateRequest;
import com.coltrip.backend.review.dto.ReviewListResponse;
import com.coltrip.backend.review.dto.ReviewResponse;
import com.coltrip.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 방문 기록이 작성 자격의 근거라 visitId 하위에 둔다 (방문 1건당 리뷰 1건)
    @PostMapping("/api/visits/{visitId}/review")
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal Long userId,
                                                   @PathVariable Long visitId,
                                                   @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.create(userId, visitId, request));
    }

    @GetMapping("/api/spots/{spotId}/reviews")
    public ResponseEntity<ReviewListResponse> findBySpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(reviewService.findBySpot(spotId));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long reviewId) {
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok("리뷰 삭제 완료");
    }
}
