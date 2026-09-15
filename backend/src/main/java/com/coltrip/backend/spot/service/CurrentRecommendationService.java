package com.coltrip.backend.spot.service;

import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.forecast.ForecastPolicy;
import com.coltrip.backend.spot.dto.CurrentRecommendationResponse;
import com.coltrip.backend.spot.dto.SpotSummaryResponse;
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

    // 위치기반서비스사업자 등록 이슈로 서버는 위치를 받지 않는다. 필터에 맞는 전체 장소를 고요지수 순으로 반환한다.
    public CurrentRecommendationResponse recommend(Long userId, Category category, Mode mode, int limit) {
        policy.validateLimit(limit);
        var ranked = spots.findByFilters(category, mode).stream()
                .sorted(Comparator.comparing(TouristSpot::getCurrentQuietScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TouristSpot::getId))
                .limit(limit).toList();
        Set<Long> liked = userId == null || ranked.isEmpty() ? Set.of()
                : likes.findLikedSpotIds(userId, ranked.stream().map(TouristSpot::getId).toList());
        var result = ranked.stream().map(spot -> new CurrentRecommendationResponse.Item(
                SpotSummaryResponse.from(spot, liked.contains(spot.getId()))))
                .toList();
        return new CurrentRecommendationResponse("CURRENT", "Asia/Seoul", "QUIET_DESC", result,
                result.isEmpty() ? "현재 조건에 맞는 장소가 없습니다." : "현재 저장된 고요지수가 높은 순서입니다.");
    }
}
