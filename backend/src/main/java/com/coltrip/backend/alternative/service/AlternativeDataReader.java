package com.coltrip.backend.alternative.service;

import com.coltrip.backend.alternative.dto.AlternativeListResponse.Place;
import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlternativeDataReader {

    // AI mock 데이터와 백엔드 시드에 함께 존재하는 장소만 연결한다.
    private static final Map<String, String> MOCK_IDS = Map.of(
            "SEED-008", "POI001", "SEED-002", "POI004",
            "SEED-001", "POI005", "SEED-004", "POI008");

    // 실제 AI poiId는 TourAPI contentId 그대로인 순수 숫자 문자열이다(docs/spot-import-api-spec.md).
    // SEED-/TEST- 등 개발용 접두어를 블랙리스트로 막는 대신, 이 규칙을 화이트리스트로 검증해
    // 앞으로 어떤 접두어의 더미 데이터가 들어와도 AI로 새어나가지 않게 한다.
    private static final Pattern REAL_AI_ID = Pattern.compile("^[0-9]+$");

    private final TouristSpotRepository spotRepository;
    private final VisitRepository visitRepository;
    private final SpotLikeRepository likeRepository;
    private final AiProperties properties;

    public Target loadTarget(Long userId, Long spotId) {
        TouristSpot spot = spotRepository.findById(spotId).orElseThrow(SpotNotFoundException::new);
        String aiPoiId = toAiId(spot.getTourApiContentId());
        if (aiPoiId == null) {
            throw new AiIntegrationException(HttpStatus.CONFLICT,
                    "AiPoiNotMapped", "현재 AI 데이터 모드와 연결된 장소가 아닙니다.");
        }
        Double baseline = null;
        if (userId != null) {
            baseline = visitRepository.findByUserIdAndStatusWithSpot(userId, VisitStatus.STARTED)
                    .stream()
                    .filter(visit -> visit.getSpot().getId().equals(spotId))
                    .findFirst()
                    .map(visit -> visit.getStartQuietScore())
                    .map(Integer::doubleValue)
                    .orElse(null);
        }
        return new Target(spotId, aiPoiId, spot.getLatitude(), spot.getLongitude(), baseline);
    }

    public Map<String, Place> loadPlaces(Long userId, List<String> aiIds) {
        List<String> contentIds = aiIds.stream().map(this::toBackendId)
                .filter(Objects::nonNull).distinct().toList();
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        List<TouristSpot> spots = spotRepository.findByTourApiContentIdsWithModes(contentIds);
        Set<Long> liked = userId == null || spots.isEmpty() ? Set.of()
                : likeRepository.findLikedSpotIds(userId, spots.stream().map(TouristSpot::getId).toList());
        Map<String, Place> result = new LinkedHashMap<>();
        for (TouristSpot spot : spots) {
            String aiId = toAiId(spot.getTourApiContentId());
            if (aiId != null) {
                result.put(aiId, new Place(spot.getId(), spot.getName(), spot.getAddress(),
                        spot.getCategory().name(), spot.getModes().stream().map(Enum::name).toList(),
                        spot.getImageUrl(), spot.getLatitude(), spot.getLongitude(),
                        liked.contains(spot.getId())));
            }
        }
        return result;
    }

    private String toAiId(String contentId) {
        if ("mock".equals(properties.dataSource())) {
            return MOCK_IDS.get(contentId);
        }
        return REAL_AI_ID.matcher(contentId).matches() ? contentId : null;
    }

    private String toBackendId(String aiId) {
        if ("mock".equals(properties.dataSource())) {
            return MOCK_IDS.entrySet().stream().filter(entry -> entry.getValue().equals(aiId))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
        }
        return REAL_AI_ID.matcher(aiId).matches() ? aiId : null;
    }

    public record Target(Long spotId, String aiPoiId, BigDecimal latitude,
                         BigDecimal longitude, Double baselineQuietIndex) {
    }
}
