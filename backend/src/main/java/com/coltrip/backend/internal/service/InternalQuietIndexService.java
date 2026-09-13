package com.coltrip.backend.internal.service;

import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.QuietIndex;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.internal.dto.QuietIndexPushRequest;
import com.coltrip.backend.internal.dto.QuietIndexPushResponse;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalQuietIndexService {

    private final TouristSpotRepository touristSpotRepository;
    private final QuietIndexRepository quietIndexRepository;
    private final InternalApiProperties internalApiProperties;

    public QuietIndexPushResponse push(String apiKey, QuietIndexPushRequest request) {
        validateApiKey(apiKey);

        // 같은 장소에 대한 동시 push를 직렬화해, 이력 upsert와 캐시 갱신이 함께 원자적으로 처리되게 한다.
        TouristSpot spot = touristSpotRepository.findByTourApiContentIdForUpdate(request.tourApiContentId())
                .orElseThrow(SpotNotFoundException::new);

        upsertHistory(spot, request);
        spot.updateQuietScoreIfNewer(request.quietScore(), request.calculatedAt());

        return new QuietIndexPushResponse(spot.getId(), spot.getCurrentQuietScore(), spot.getQuietLevel().name());
    }

    // 같은 장소·같은 계산 시각의 재전송은 이력을 새로 쌓지 않고 기존 이력을 정정한다.
    private void upsertHistory(TouristSpot spot, QuietIndexPushRequest request) {
        quietIndexRepository.findBySpot_IdAndCalculatedAt(spot.getId(), request.calculatedAt())
                .ifPresentOrElse(
                        existing -> existing.correct(request.quietScore(), request.rawMetrics()),
                        () -> quietIndexRepository.save(QuietIndex.builder()
                                .spot(spot)
                                .quietScore(request.quietScore())
                                .rawMetrics(request.rawMetrics())
                                .calculatedAt(request.calculatedAt())
                                .build()));
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(internalApiProperties.apiKey())) {
            throw new InvalidInternalApiKeyException();
        }
    }
}
