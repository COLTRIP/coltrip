package com.coltrip.backend.spot.controller;

import com.coltrip.backend.common.exception.ErrorResponse;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.forecast.InvalidForecastRequestException;
import com.coltrip.backend.spot.dto.CurrentRecommendationResponse;
import com.coltrip.backend.spot.service.CurrentRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "현재 고요지수 추천")
@RestController
@RequiredArgsConstructor
public class CurrentRecommendationController {
    private final CurrentRecommendationService service;

    @Operation(summary = "현재 고요지수 기준 추천", description = """
            최초 진입 및 현재 모드 복귀용입니다. 날짜/시간 예측 API와 별개입니다.
            지도/상세와 같은 저장값을 사용하며 type=CURRENT, spots[].spot.quietScore로 반환합니다.
            위치기반서비스사업자 등록 이슈로 서버는 위치를 받지 않으며, category/mode 필터에 맞는 전체 장소를 대상으로 합니다.
            limit 1~50(기본 20). 점수 내림차순(null 마지막), ID 오름차순이며 미수신 점수는 0이나 예측값으로 대체하지 않습니다.
            실제 실시간 실측을 보장하지 않습니다. quietScoreUpdatedAt을 함께 확인하세요.
            비로그인 허용, 유효한 토큰은 isLiked에 반영합니다. 결과 없음은 200과 spots=[]입니다.
            """)
    @ApiResponse(responseCode = "200", description = "현재값 추천 목록 및 안내 문구")
    @ApiResponse(responseCode = "400", description = "개수 또는 파라미터 형식 오류")
    @SecurityRequirements
    @GetMapping("/api/spots/recommendations/current")
    public ResponseEntity<CurrentRecommendationResponse> recommend(@AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Mode mode,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                service.recommend(userId, category, mode, limit));
    }

    @ExceptionHandler(InvalidForecastRequestException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidForecastRequestException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("InvalidCurrentRecommendationRequest", exception.getMessage()));
    }
}
