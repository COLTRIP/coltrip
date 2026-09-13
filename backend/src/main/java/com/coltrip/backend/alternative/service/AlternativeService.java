package com.coltrip.backend.alternative.service;

import com.coltrip.backend.alternative.client.AiAlternativeClient;
import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.alternative.dto.AlternativeListResponse.Alternative;
import com.coltrip.backend.alternative.dto.AlternativeListResponse.Place;
import com.coltrip.backend.alternative.service.AlternativeDataReader.Target;
import com.coltrip.backend.common.util.GeoUtils;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AlternativeService {

    private final AlternativeDataReader dataReader;
    private final AiAlternativeClient aiClient;
    private final Clock clock;

    public AlternativeService(AlternativeDataReader dataReader, AiAlternativeClient aiClient,
                              @Qualifier("alternativeClock") Clock clock) {
        this.dataReader = dataReader;
        this.aiClient = aiClient;
        this.clock = clock;
    }

    // 외부 AI 호출을 DB 트랜잭션 안에서 기다리지 않도록 읽기를 분리한다.
    public AlternativeListResponse find(Long userId, Long spotId) {
        Target target = dataReader.loadTarget(userId, spotId);
        ZonedDateTime now = ZonedDateTime.now(clock);
        boolean weekend = now.getDayOfWeek() == DayOfWeek.SATURDAY
                || now.getDayOfWeek() == DayOfWeek.SUNDAY;
        AiAlternativeClient.Result result = aiClient.fetch(new AiAlternativeClient.Request(
                target.aiPoiId(), now.getHour(), weekend, target.baselineQuietIndex()));

        if (!result.triggered()) {
            return new AlternativeListResponse(false, result.targetQuietIndex(), List.of(),
                    "현재 장소는 대체지 제안 조건에 해당하지 않습니다.");
        }
        if (result.alternatives().isEmpty()) {
            return empty(result.targetQuietIndex());
        }

        Map<String, Place> places = dataReader.loadPlaces(userId,
                result.alternatives().stream().map(AiAlternativeClient.Candidate::poiId).toList());
        Map<Long, Alternative> unique = new LinkedHashMap<>();
        for (AiAlternativeClient.Candidate candidate : result.alternatives()) {
            Place place = places.get(candidate.poiId());
            if (place == null || place.id().equals(spotId)
                    || candidate.quietIndex() <= result.targetQuietIndex() || candidate.distanceKm() > 3.0) {
                continue;
            }
            double meters = GeoUtils.distanceMeters(target.latitude(), target.longitude(),
                    place.latitude(), place.longitude());
            if (!Double.isFinite(meters) || meters > 3000) {
                continue;
            }
            Alternative item = new Alternative(place, candidate.quietIndex(),
                    Math.round(meters / 10.0) / 100.0, candidate.score(), candidate.recommendReason());
            unique.merge(place.id(), item, (left, right) -> left.score() >= right.score() ? left : right);
        }
        List<Alternative> alternatives = unique.values().stream()
                .sorted(Comparator.comparingDouble(Alternative::score).reversed()).limit(3).toList();
        return alternatives.isEmpty() ? empty(result.targetQuietIndex())
                : new AlternativeListResponse(true, result.targetQuietIndex(), alternatives,
                        "주변의 더 한적한 대체 장소입니다.");
    }

    private AlternativeListResponse empty(double targetQuietIndex) {
        return new AlternativeListResponse(true, targetQuietIndex, List.of(),
                "반경 3km 안에서 표시할 수 있는 등록된 대체 장소가 없습니다.");
    }
}
