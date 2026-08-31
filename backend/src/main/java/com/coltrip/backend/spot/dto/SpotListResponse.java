package com.coltrip.backend.spot.dto;

import com.coltrip.backend.domain.spot.TouristSpot;
import java.util.List;

public record SpotListResponse(
        List<SpotSummaryResponse> spots
) {
    public static SpotListResponse from(List<TouristSpot> spots) {
        return new SpotListResponse(spots.stream().map(SpotSummaryResponse::from).toList());
    }
}
