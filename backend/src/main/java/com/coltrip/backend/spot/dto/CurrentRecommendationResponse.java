package com.coltrip.backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentRecommendationResponse(
        @Schema(description = "현재 저장값 구분", allowableValues = "CURRENT") String type,
        String timezone, String sort,
        List<Item> spots, String message) {
    public record Item(SpotSummaryResponse spot) { }
}
