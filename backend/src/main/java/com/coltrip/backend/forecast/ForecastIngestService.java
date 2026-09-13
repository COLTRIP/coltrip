package com.coltrip.backend.forecast;

import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.forecast.QuietForecast;
import com.coltrip.backend.domain.forecast.QuietForecastRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ForecastIngestService {
    private final TouristSpotRepository spots;
    private final QuietForecastRepository forecasts;
    private final InternalApiProperties internal;
    private final ForecastPolicy policy;
    private final String source;
    private final Clock clock;

    @Autowired
    public ForecastIngestService(TouristSpotRepository spots, QuietForecastRepository forecasts,
            InternalApiProperties internal, ForecastPolicy policy,
            @Value("${forecast.source:coltrip-ai}") String source) {
        this(spots, forecasts, internal, policy, source, Clock.system(ForecastPolicy.ZONE));
    }

    ForecastIngestService(TouristSpotRepository spots, QuietForecastRepository forecasts,
            InternalApiProperties internal, ForecastPolicy policy, String source, Clock clock) {
        this.spots = spots;
        this.forecasts = forecasts;
        this.internal = internal;
        this.policy = policy;
        this.source = source;
        this.clock = clock;
    }

    public PushResult receive(String apiKey, ForecastPushRequest request) {
        if (!StringUtils.hasText(internal.apiKey()) || !internal.apiKey().equals(apiKey)) {
            throw new InvalidInternalApiKeyException();
        }
        if (!source.equals(request.source())) {
            throw new InvalidForecastRequestException("허용된 forecast.source와 일치하지 않습니다.");
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        LocalDateTime generated = policy.local(request.generatedAt());
        if (generated.isAfter(now)) {
            throw new InvalidForecastRequestException("generatedAt은 미래 시각일 수 없습니다.");
        }
        Set<Key> seen = new HashSet<>();
        for (var item : request.forecasts()) {
            LocalDateTime target = policy.local(item.targetAt());
            LocalDateTime validUntil = policy.local(item.validUntil());
            if (item.targetAt().getSecond() != 0 || item.targetAt().getNano() != 0
                    || target.getMinute() != 0 || target.isBefore(generated.truncatedTo(ChronoUnit.HOURS))
                    || target.isAfter(generated.truncatedTo(ChronoUnit.HOURS).plusDays(7))) {
                throw new InvalidForecastRequestException("targetAt은 생성 시각의 시간 슬롯부터 7일 이내의 한국 시간 정시여야 합니다.");
            }
            if (!validUntil.isAfter(generated) || validUntil.isAfter(target.plusHours(1))) {
                throw new InvalidForecastRequestException("validUntil은 generatedAt 이후이며 대상 슬롯 종료 이하이어야 합니다.");
            }
            if (!seen.add(new Key(item.tourApiContentId(), target))) {
                throw new InvalidForecastRequestException("동일 배치에 중복된 장소/대상 시각이 있습니다.");
            }
        }
        int created = 0;
        int updated = 0;
        // 배치 간 역순 잠금으로 교착되지 않도록 장소 ID 순서를 고정한다.
        var items = request.forecasts().stream().sorted(Comparator.comparing(ForecastPushRequest.Item::tourApiContentId)
                .thenComparing(item -> policy.local(item.targetAt()))).toList();
        for (var item : items) {
            var spot = spots.findByTourApiContentIdForUpdate(item.tourApiContentId())
                    .orElseThrow(SpotNotFoundException::new);
            LocalDateTime target = policy.local(item.targetAt());
            var existing = forecasts.findBySpot_IdAndTargetAtAndSourceAndGeneratedAt(spot.getId(), target, source, generated);
            QuietForecast forecast;
            if (existing.isPresent()) {
                forecast = existing.get();
                updated++;
            } else {
                forecast = new QuietForecast(spot, target, generated, source);
                created++;
            }
            forecast.correct(item.quietIndex().setScale(2, RoundingMode.HALF_UP),
                    policy.local(item.validUntil()), now, request.modelVersion());
            forecasts.save(forecast);
        }
        return new PushResult(created, updated);
    }

    private record Key(String contentId, LocalDateTime targetAt) { }
    public record PushResult(int created, int updated) { }
}
