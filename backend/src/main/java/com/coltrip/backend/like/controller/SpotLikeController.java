package com.coltrip.backend.like.controller;

import com.coltrip.backend.like.dto.LikeResponse;
import com.coltrip.backend.like.service.SpotLikeService;
import com.coltrip.backend.spot.dto.SpotListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SpotLikeController {

    private final SpotLikeService spotLikeService;

    @PostMapping("/api/spots/{spotId}/like")
    public ResponseEntity<LikeResponse> like(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long spotId) {
        return ResponseEntity.ok(spotLikeService.like(userId, spotId));
    }

    @DeleteMapping("/api/spots/{spotId}/like")
    public ResponseEntity<LikeResponse> unlike(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long spotId) {
        return ResponseEntity.ok(spotLikeService.unlike(userId, spotId));
    }

    @GetMapping("/api/users/me/likes")
    public ResponseEntity<SpotListResponse> findMyLikedSpots(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(spotLikeService.findMyLikedSpots(userId));
    }
}
