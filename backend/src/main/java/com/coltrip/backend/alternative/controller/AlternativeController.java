package com.coltrip.backend.alternative.controller;

import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.alternative.service.AlternativeService;
import com.coltrip.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대체 장소 추천")
@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class AlternativeController {

    private final AlternativeService alternativeService;

    @Operation(summary = "대체 장소 추천 조회", description = """
            비로그인 조회 가능. 로그인 사용자가 해당 장소를 방문 중이면 시작 고요지수를 AI에 전달합니다.
            원래 목적지 기준 3km 이내의 등록된 장소를 최대 3개 반환합니다.
            score는 거리 감점을 포함한 추천 점수이며 순수 유사도가 아닙니다.
            AI 장애는 502/504, AI 설정 누락은 503, 장소 매핑 불가는 409로 반환합니다.
            """)
    @SecurityRequirements
    @GetMapping("/{spotId}/alternatives")
    public ResponseEntity<AlternativeListResponse> find(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long spotId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(alternativeService.find(userId, spotId));
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleAiError(AiIntegrationException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }
}
