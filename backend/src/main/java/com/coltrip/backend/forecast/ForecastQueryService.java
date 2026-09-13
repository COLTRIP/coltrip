package com.coltrip.backend.forecast;

import com.coltrip.backend.common.util.GeoUtils;
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

    public Recommendations recommend(Long userId, LocalDate date, Integer hour, BigDecimal latitude,
            BigDecimal longitude, int radiusMeters, Category category, Mode mode, int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime target = policy.target(date, hour, now);
        var area = policy.area(latitude, longitude, radiusMeters, limit);
        List<Ranked> ranked = forecasts.findCandidates(target, source, now,
                        area.south(), area.north(), area.west(), area.east(), category, mode).stream()
                .map(f -> new Ranked(f, GeoUtils.distanceMeters(area.latitude(), area.longitude(),
                        f.getSpot().getLatitude(), f.getSpot().getLongitude())))
                .filter(f -> Double.isFinite(f.distance()) && f.distance() <= radiusMeters)
                .sorted(Comparator.<Ranked, BigDecimal>comparing(f -> f.forecast().getQuietIndex()).reversed()
                        .thenComparingDouble(Ranked::distance).thenComparing(f -> f.forecast().getSpot().getId()))
                .limit(limit).toList();
        Set<Long> liked = userId == null || ranked.isEmpty() ? Set.of()
                : likes.findLikedSpotIds(userId, ranked.stream().map(f -> f.forecast().getSpot().getId()).toList());
        List<Item> result = ranked.stream().map(r -> {
            var spot = r.forecast().getSpot();
            Place place = new Place(spot.getId(), spot.getName(), spot.getAddress(), spot.getCategory().name(),
                    spot.getModes().stream().map(Enum::name).toList(), spot.getImageUrl(), spot.getLatitude(),
                    spot.getLongitude(), liked.contains(spot.getId()));
            return new Item(place, Math.round(r.distance()), Point.from(r.forecast()));
        }).toList();
        return new Recommendations("Asia/Seoul", ForecastResponses.offset(target), area.latitude(), area.longitude(),
                radiusMeters, area.defaultCenter(), "QUIET_DESC", result,
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

    private record Ranked(QuietForecast forecast, double distance) { }
}
