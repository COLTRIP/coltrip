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
import com.coltrip.backend.internal.exception.InvalidObservationTimeException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class InternalQuietIndexService {

    private final TouristSpotRepository touristSpotRepository;
    private final QuietIndexRepository quietIndexRepository;
    private final InternalApiProperties internalApiProperties;
    private final Clock clock;

    @Autowired
    public InternalQuietIndexService(TouristSpotRepository spots, QuietIndexRepository history,
                                     InternalApiProperties properties) {
        this(spots, history, properties, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    InternalQuietIndexService(TouristSpotRepository spots, QuietIndexRepository history,
                              InternalApiProperties properties, Clock clock) {
        this.touristSpotRepository = spots;
        this.quietIndexRepository = history;
        this.internalApiProperties = properties;
        this.clock = clock;
    }

    public QuietIndexPushResponse push(String apiKey, QuietIndexPushRequest request) {
        validateApiKey(apiKey);

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"));
        if (request.calculatedAt() == null || request.calculatedAt().isAfter(now)) {
            throw new InvalidObservationTimeException();
        }

        // 같은 장소에 대한 동시 push를 직렬화해, 이력 upsert와 캐시 갱신이 함께 원자적으로 처리되게 한다.
        TouristSpot spot = touristSpotRepository.findByTourApiContentIdForUpdate(request.tourApiContentId())
                .orElseThrow(SpotNotFoundException::new);

        QuietIndexCacheWriter.upsert(quietIndexRepository, spot,
                request.quietScore(), request.calculatedAt(), request.rawMetrics());

        return new QuietIndexPushResponse(spot.getId(), spot.getCurrentQuietScore(), spot.getQuietLevel().name());
    }

    private void validateApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(internalApiProperties.apiKey())
                || !apiKey.equals(internalApiProperties.apiKey())) {
            throw new InvalidInternalApiKeyException();
        }
    }
}
