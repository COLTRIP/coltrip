package com.coltrip.backend.internal.service;

import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.QuietIndexCacheWriter;
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

        QuietIndexCacheWriter.upsert(quietIndexRepository, spot,
                request.quietScore(), request.calculatedAt(), request.rawMetrics());

        return new QuietIndexPushResponse(spot.getId(), spot.getCurrentQuietScore(), spot.getQuietLevel().name());
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(internalApiProperties.apiKey())) {
            throw new InvalidInternalApiKeyException();
        }
    }
}
