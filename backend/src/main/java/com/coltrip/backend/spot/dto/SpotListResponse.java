package com.coltrip.backend.spot.dto;

import com.coltrip.backend.domain.spot.TouristSpot;
import java.util.List;
import java.util.Set;

public record SpotListResponse(
        List<SpotSummaryResponse> spots
) {
    public static SpotListResponse from(List<TouristSpot> spots, Set<Long> likedSpotIds) {
        return new SpotListResponse(spots.stream()
                .map(spot -> SpotSummaryResponse.from(spot, likedSpotIds.contains(spot.getId())))
                .toList());
    }

    // 좋아요한 장소 목록 조회처럼 결과가 전부 좋아요한 장소일 때 사용
    public static SpotListResponse allLiked(List<TouristSpot> spots) {
        return new SpotListResponse(spots.stream()
                .map(spot -> SpotSummaryResponse.from(spot, true))
                .toList());
    }
}
