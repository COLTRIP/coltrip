package com.coltrip.backend.review.controller;

import com.coltrip.backend.review.dto.ReviewCreateRequest;
import com.coltrip.backend.review.dto.ReviewListResponse;
import com.coltrip.backend.review.dto.ReviewResponse;
import com.coltrip.backend.review.dto.ReviewUpdateRequest;
import com.coltrip.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰", description = "별점(1~5) + 한줄평. 방문 완료자만 작성 가능")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 방문 기록이 작성 자격의 근거라 visitId 하위에 둔다 (방문 1건당 리뷰 1건)
    @Operation(summary = "리뷰 작성",
            description = """
                    `rating`(1~5 별점)과 `content`(한줄평, 선택·최대 300자)를 받습니다.

                    **방문 완료(COMPLETED)한 사용자만**, **방문 1건당 1개**만 작성 가능합니다.
                    그래서 경로가 spots가 아니라 visits 하위입니다.
                    """)
    @PostMapping("/api/visits/{visitId}/review")
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal Long userId,
                                                   @PathVariable Long visitId,
                                                   @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(reviewService.create(userId, visitId, request));
    }

    @Operation(summary = "장소별 리뷰 목록", description = "인증 불필요. 최신순.")
    @SecurityRequirements
    @GetMapping("/api/spots/{spotId}/reviews")
    public ResponseEntity<ReviewListResponse> findBySpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(reviewService.findBySpot(spotId));
    }

    @Operation(summary = "리뷰 수정",
            description = """
                    본인이 작성한 리뷰만 수정할 수 있습니다. 없는 리뷰/타인 리뷰는 삭제 API와 동일하게 404입니다.

                    `rating`은 필수, `content`는 생략하거나 명시적으로 null을 보내면 한줄평이 삭제됩니다
                    (부분 수정이 아니라 매번 두 필드 전체를 다시 지정하는 방식 — 닉네임 수정 API와 동일한 정책).
                    createdAt은 유지되고 updatedAt만 갱신됩니다.
                    """)
    @PatchMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> update(@AuthenticationPrincipal Long userId,
                                                  @PathVariable Long reviewId,
                                                  @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.update(userId, reviewId, request));
    }

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰만 삭제할 수 있습니다.")
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long reviewId) {
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok("리뷰 삭제 완료");
    }
}
