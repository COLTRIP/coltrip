package com.coltrip.backend.like.controller;

import com.coltrip.backend.like.dto.LikeResponse;
import com.coltrip.backend.like.service.SpotLikeService;
import com.coltrip.backend.spot.dto.SpotListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "좋아요", description = "관광지 좋아요 등록/취소/목록")
@RestController
@RequiredArgsConstructor
public class SpotLikeController {

    private final SpotLikeService spotLikeService;

    @Operation(summary = "좋아요 등록", description = "멱등. 이미 눌렀어도 200을 반환합니다.")
    @PostMapping("/api/spots/{spotId}/like")
    public ResponseEntity<LikeResponse> like(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long spotId) {
        return ResponseEntity.ok(spotLikeService.like(userId, spotId));
    }

    @Operation(summary = "좋아요 취소", description = "멱등. 누르지 않은 상태여도 200을 반환합니다.")
    @DeleteMapping("/api/spots/{spotId}/like")
    public ResponseEntity<LikeResponse> unlike(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long spotId) {
        return ResponseEntity.ok(spotLikeService.unlike(userId, spotId));
    }

    @Operation(summary = "내가 좋아요한 장소 목록", description = "최근 좋아요순. 응답 형태는 관광지 목록 조회와 동일합니다.")
    @GetMapping("/api/users/me/likes")
    public ResponseEntity<SpotListResponse> findMyLikedSpots(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(spotLikeService.findMyLikedSpots(userId));
    }
}
