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

        TouristSpot spot = touristSpotRepository.findByTourApiContentId(request.tourApiContentId())
                .orElseThrow(SpotNotFoundException::new);

        quietIndexRepository.save(QuietIndex.builder()
                .spot(spot)
                .quietScore(request.quietScore())
                .rawMetrics(request.rawMetrics())
                .calculatedAt(request.calculatedAt())
                .build());

        spot.updateQuietScore(request.quietScore(), request.calculatedAt());

        return new QuietIndexPushResponse(spot.getId(), spot.getCurrentQuietScore(), spot.getQuietLevel().name());
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(internalApiProperties.apiKey())) {
            throw new InvalidInternalApiKeyException();
        }
    }
}
