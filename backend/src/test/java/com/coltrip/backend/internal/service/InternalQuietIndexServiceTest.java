package com.coltrip.backend.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.QuietIndex;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.internal.dto.QuietIndexPushRequest;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalQuietIndexServiceTest {

    private static final String API_KEY = "internal-key";
    private static final String CONTENT_ID = "TOURAPI-1";

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private QuietIndexRepository quietIndexRepository;

    private final InternalApiProperties internalApiProperties = new InternalApiProperties(API_KEY);

    @Test
    void rejectsInvalidApiKey() {
        InternalQuietIndexService service =
                new InternalQuietIndexService(touristSpotRepository, quietIndexRepository, internalApiProperties);
        QuietIndexPushRequest request = new QuietIndexPushRequest(CONTENT_ID, 80, LocalDateTime.now(), null);

        assertThrows(InvalidInternalApiKeyException.class, () -> service.push("wrong-key", request));
        verify(touristSpotRepository, never()).findByTourApiContentIdForUpdate(any());
    }

    @Test
    void savesNewHistoryRowWhenNoneExistsForSameCalculatedAt() {
        InternalQuietIndexService service =
                new InternalQuietIndexService(touristSpotRepository, quietIndexRepository, internalApiProperties);
        TouristSpot spot = newSpot();
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 10, 10, 0);
        QuietIndexPushRequest request = new QuietIndexPushRequest(CONTENT_ID, 80, calculatedAt, "raw");

        when(touristSpotRepository.findByTourApiContentIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(spot));
        when(quietIndexRepository.findBySpot_IdAndCalculatedAt(spot.getId(), calculatedAt)).thenReturn(Optional.empty());

        service.push(API_KEY, request);

        verify(quietIndexRepository).save(any(QuietIndex.class));
        assertEquals(80, spot.getCurrentQuietScore());
        assertEquals(calculatedAt, spot.getQuietScoreUpdatedAt());
    }

    @Test
    void correctsExistingHistoryRowInsteadOfInsertingDuplicateOnResend() {
        InternalQuietIndexService service =
                new InternalQuietIndexService(touristSpotRepository, quietIndexRepository, internalApiProperties);
        TouristSpot spot = newSpot();
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 10, 10, 0);
        QuietIndex existing = QuietIndex.builder()
                .spot(spot).quietScore(80).calculatedAt(calculatedAt).build();
        QuietIndexPushRequest request = new QuietIndexPushRequest(CONTENT_ID, 55, calculatedAt, "corrected");

        when(touristSpotRepository.findByTourApiContentIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(spot));
        when(quietIndexRepository.findBySpot_IdAndCalculatedAt(spot.getId(), calculatedAt)).thenReturn(Optional.of(existing));

        service.push(API_KEY, request);

        verify(quietIndexRepository, never()).save(any(QuietIndex.class));
        assertEquals(55, existing.getQuietScore());
        assertEquals("corrected", existing.getRawMetrics());
    }

    @Test
    void staleCalculatedAtDoesNotOverwriteCurrentCache() {
        InternalQuietIndexService service =
                new InternalQuietIndexService(touristSpotRepository, quietIndexRepository, internalApiProperties);
        TouristSpot spot = newSpot();
        spot.updateQuietScoreIfNewer(80, LocalDateTime.of(2026, 9, 10, 10, 0));
        LocalDateTime staleAt = LocalDateTime.of(2026, 9, 10, 9, 0);
        QuietIndexPushRequest request = new QuietIndexPushRequest(CONTENT_ID, 30, staleAt, null);

        when(touristSpotRepository.findByTourApiContentIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(spot));
        when(quietIndexRepository.findBySpot_IdAndCalculatedAt(spot.getId(), staleAt)).thenReturn(Optional.empty());

        service.push(API_KEY, request);

        assertEquals(80, spot.getCurrentQuietScore());
        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 0), spot.getQuietScoreUpdatedAt());
    }

    @Test
    void unknownSpotThrows() {
        InternalQuietIndexService service =
                new InternalQuietIndexService(touristSpotRepository, quietIndexRepository, internalApiProperties);
        QuietIndexPushRequest request = new QuietIndexPushRequest(CONTENT_ID, 80, LocalDateTime.now(), null);

        when(touristSpotRepository.findByTourApiContentIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(SpotNotFoundException.class, () -> service.push(API_KEY, request));
    }

    private TouristSpot newSpot() {
        return TouristSpot.builder()
                .tourApiContentId(CONTENT_ID)
                .name("테스트 장소")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.0))
                .longitude(BigDecimal.valueOf(129.0))
                .category(Category.CAFE)
                .build();
    }
}
