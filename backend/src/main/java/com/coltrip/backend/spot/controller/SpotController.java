package com.coltrip.backend.spot.controller;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.spot.dto.SpotDetailResponse;
import com.coltrip.backend.spot.dto.SpotListResponse;
import com.coltrip.backend.spot.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관광지", description = "지도/목록 조회 (인증 불필요)")
@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @Operation(summary = "관광지 목록 조회",
            description = """
                    지도에 보이는 영역(bounding box) 안의 관광지를 조회합니다. 인증 불필요(토큰이 있으면 isLiked에 반영).

                    `mode`는 필터 조건일 뿐이며, 응답의 `modes`에는 해당 장소가 가진 모든 감성모드가 담깁니다.
                    `quietLevel`은 `quietScore`에서 파생됩니다 (0~40 CROWDED, 41~70 NORMAL, 71~100 QUIET).
                    """)
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<SpotListResponse> findInBounds(@AuthenticationPrincipal Long userId,
                                                           @RequestParam BigDecimal swLat,
                                                           @RequestParam BigDecimal swLng,
                                                           @RequestParam BigDecimal neLat,
                                                           @RequestParam BigDecimal neLng,
                                                           @RequestParam(required = false) Category category,
                                                           @RequestParam(required = false) Mode mode) {
        return ResponseEntity.ok(spotService.findInBounds(userId, swLat, swLng, neLat, neLng, category, mode));
    }

    @Operation(summary = "관광지 상세 조회",
            description = "인증 불필요(토큰이 있으면 isLiked에 반영). 목록 응답에 없는 description, recommendReason이 추가로 담깁니다.")
    @SecurityRequirements
    @GetMapping("/{spotId}")
    public ResponseEntity<SpotDetailResponse> findById(@AuthenticationPrincipal Long userId,
                                                         @PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.findById(userId, spotId));
    }
}
