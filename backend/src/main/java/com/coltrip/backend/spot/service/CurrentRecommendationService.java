package com.coltrip.backend.spot.service;

import com.coltrip.backend.common.util.GeoUtils;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.forecast.ForecastPolicy;
import com.coltrip.backend.spot.dto.CurrentRecommendationResponse;
import com.coltrip.backend.spot.dto.SpotSummaryResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentRecommendationService {
    private final TouristSpotRepository spots;
    private final SpotLikeRepository likes;
    private final ForecastPolicy policy;

    public CurrentRecommendationResponse recommend(Long userId, BigDecimal latitude, BigDecimal longitude,
            int radiusMeters, Category category, Mode mode, int limit) {
        // 예측 추천과 위치 기본값/반경 검증을 공유하되 예측 저장소나 AI는 조회하지 않는다.
        var area = policy.area(latitude, longitude, radiusMeters, limit);
        var ranked = spots.findInBounds(area.south(), area.north(), area.west(), area.east(), category, mode)
                .stream().map(spot -> new Ranked(spot, GeoUtils.distanceMeters(area.latitude(), area.longitude(),
                        spot.getLatitude(), spot.getLongitude())))
                .filter(r -> Double.isFinite(r.distance()) && r.distance() <= radiusMeters)
                .sorted(Comparator.comparing((Ranked r) -> r.spot().getCurrentQuietScore(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingDouble(Ranked::distance).thenComparing(r -> r.spot().getId()))
                .limit(limit).toList();
        Set<Long> liked = userId == null || ranked.isEmpty() ? Set.of()
                : likes.findLikedSpotIds(userId, ranked.stream().map(r -> r.spot().getId()).toList());
        var result = ranked.stream().map(r -> new CurrentRecommendationResponse.Item(
                SpotSummaryResponse.from(r.spot(), liked.contains(r.spot().getId())), Math.round(r.distance())))
                .toList();
        return new CurrentRecommendationResponse("CURRENT", "Asia/Seoul", area.latitude(), area.longitude(),
                radiusMeters, area.defaultCenter(), "QUIET_DESC", result,
                result.isEmpty() ? "현재 조건에 맞는 장소가 없습니다." : "현재 저장된 고요지수가 높은 순서입니다.");
    }

    private record Ranked(TouristSpot spot, double distance) { }
}
