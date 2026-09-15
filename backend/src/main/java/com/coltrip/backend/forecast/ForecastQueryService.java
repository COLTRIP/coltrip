package com.coltrip.backend.forecast;

import com.coltrip.backend.domain.forecast.QuietForecast;
import com.coltrip.backend.domain.forecast.QuietForecastRepository;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.forecast.ForecastResponses.*;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ForecastQueryService {
    private final QuietForecastRepository forecasts;
    private final TouristSpotRepository spots;
    private final SpotLikeRepository likes;
    private final ForecastPolicy policy;
    private final String source;
    private final Clock clock;

    @Autowired
    public ForecastQueryService(QuietForecastRepository forecasts, TouristSpotRepository spots,
            SpotLikeRepository likes, ForecastPolicy policy, @Value("${forecast.source:coltrip-ai}") String source) {
        this(forecasts, spots, likes, policy, source, Clock.system(ForecastPolicy.ZONE));
    }

    ForecastQueryService(QuietForecastRepository forecasts, TouristSpotRepository spots,
            SpotLikeRepository likes, ForecastPolicy policy, String source, Clock clock) {
        this.forecasts = forecasts;
        this.spots = spots;
        this.likes = likes;
        this.policy = policy;
        this.source = source;
        this.clock = clock;
    }

    // 위치기반서비스사업자 등록 이슈로 서버는 위치를 받지 않는다. 필터에 맞는 전체 장소를 예측 고요지수 순으로 반환한다.
    public Recommendations recommend(Long userId, LocalDate date, Integer hour, Category category, Mode mode, int limit) {
        policy.validateLimit(limit);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime target = policy.target(date, hour, now);
        List<QuietForecast> ranked = forecasts.findCandidates(target, source, now, category, mode).stream()
                .sorted(Comparator.<QuietForecast, BigDecimal>comparing(QuietForecast::getQuietIndex).reversed()
                        .thenComparing(f -> f.getSpot().getId()))
                .limit(limit).toList();
        Set<Long> liked = userId == null || ranked.isEmpty() ? Set.of()
                : likes.findLikedSpotIds(userId, ranked.stream().map(f -> f.getSpot().getId()).toList());
        List<Item> result = ranked.stream().map(f -> {
            var spot = f.getSpot();
            Place place = new Place(spot.getId(), spot.getName(), spot.getAddress(), spot.getCategory().name(),
                    spot.getModes().stream().map(Enum::name).toList(), spot.getImageUrl(), spot.getLatitude(),
                    spot.getLongitude(), liked.contains(spot.getId()));
            return new Item(place, Point.from(f));
        }).toList();
        return new Recommendations("Asia/Seoul", ForecastResponses.offset(target), "QUIET_DESC", result,
                result.isEmpty() ? "선택한 시간과 조건에 맞는 유효한 예측 데이터가 없습니다." : "예측 고요지수가 높은 순서입니다.");
    }

    public Timeline timeline(Long spotId, LocalDate date, Integer hour) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = policy.target(date, hour, now);
        policy.target(start.plusHours(23).toLocalDate(), start.plusHours(23).getHour(), now);
        if (!spots.existsById(spotId)) {
            throw new SpotNotFoundException();
        }
        Map<LocalDateTime, QuietForecast> byHour = forecasts.findTimeline(spotId, start, start.plusHours(24), source, now)
                .stream().collect(Collectors.toMap(QuietForecast::getTargetAt, f -> f));
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalDateTime target = start.plusHours(i);
            QuietForecast forecast = byHour.get(target);
            points.add(forecast == null ? Point.empty(target) : Point.from(forecast));
        }
        return new Timeline("Asia/Seoul", spotId, points);
    }
}
