package com.coltrip.backend.alternative.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record AlternativeListResponse(
        boolean triggered,
        @Schema(description = "AI가 계산한 목적지 고요지수. 0~100, 높을수록 고요함")
        double targetQuietIndex,
        List<Alternative> alternatives,
        String message
) {
    public record Alternative(
            Place spot,
            @Schema(description = "AI 대체지 고요지수. 소수점 유지") double quietIndex,
            @Schema(description = "원래 목적지와 대체지의 백엔드 좌표 기준 직선거리(km)") double distanceKm,
            @Schema(description = "0~1 추천 점수. 유사도에 이동 거리 감점을 적용한 값") double score,
            String recommendReason
    ) {
    }

    public record Place(Long id, String name, String address, String category,
                        List<String> modes, String imageUrl,
                        BigDecimal latitude, BigDecimal longitude, boolean isLiked) {
    }
}
