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
        return contentId.startsWith("SEED-") ? null : contentId;
    }

    private String toBackendId(String aiId) {
        if ("mock".equals(properties.dataSource())) {
            return MOCK_IDS.entrySet().stream().filter(entry -> entry.getValue().equals(aiId))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
        }
        return aiId.startsWith("SEED-") ? null : aiId;
    }

    public record Target(Long spotId, String aiPoiId, BigDecimal latitude,
                         BigDecimal longitude, Double baselineQuietIndex) {
    }
}
