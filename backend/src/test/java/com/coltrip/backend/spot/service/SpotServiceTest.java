package com.coltrip.backend.spot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.spot.dto.SpotDetailResponse;
import com.coltrip.backend.spot.dto.SpotListResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotServiceTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal SW_LAT = BigDecimal.valueOf(35.0);
    private static final BigDecimal SW_LNG = BigDecimal.valueOf(129.0);
    private static final BigDecimal NE_LAT = BigDecimal.valueOf(35.2);
    private static final BigDecimal NE_LNG = BigDecimal.valueOf(129.2);

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private SpotLikeRepository spotLikeRepository;

    @InjectMocks
    private SpotService spotService;

    @Test
    void listMarksLikedSpotsForLoggedInUser() {
        TouristSpot spotA = newSpot(1L);
        TouristSpot spotB = newSpot(2L);
        when(touristSpotRepository.findInBounds(SW_LAT, NE_LAT, SW_LNG, NE_LNG, null, null))
                .thenReturn(List.of(spotA, spotB));
        when(spotLikeRepository.findLikedSpotIds(USER_ID, List.of(1L, 2L))).thenReturn(Set.of(1L));

        SpotListResponse response = spotService.findInBounds(USER_ID, SW_LAT, SW_LNG, NE_LAT, NE_LNG, null, null);

        assertTrue(response.spots().get(0).isLiked());
        assertFalse(response.spots().get(1).isLiked());
    }

    @Test
    void listSkipsLikeLookupForAnonymousUser() {
        TouristSpot spotA = newSpot(1L);
        when(touristSpotRepository.findInBounds(SW_LAT, NE_LAT, SW_LNG, NE_LNG, null, null))
                .thenReturn(List.of(spotA));

        SpotListResponse response = spotService.findInBounds(null, SW_LAT, SW_LNG, NE_LAT, NE_LNG, null, null);

        assertFalse(response.spots().get(0).isLiked());
        verify(spotLikeRepository, never()).findLikedSpotIds(any(), anyCollection());
    }

    @Test
    void detailReflectsLikeStatusForLoggedInUser() {
        TouristSpot spot = newSpot(1L);
        when(touristSpotRepository.findByIdWithModes(1L)).thenReturn(Optional.of(spot));
        when(spotLikeRepository.existsByUser_IdAndSpot_Id(USER_ID, 1L)).thenReturn(true);

        SpotDetailResponse response = spotService.findById(USER_ID, 1L);

        assertTrue(response.isLiked());
    }

    @Test
    void detailIsNotLikedForAnonymousUser() {
        TouristSpot spot = newSpot(1L);
        when(touristSpotRepository.findByIdWithModes(1L)).thenReturn(Optional.of(spot));

        SpotDetailResponse response = spotService.findById(null, 1L);

        assertFalse(response.isLiked());
        verify(spotLikeRepository, never()).existsByUser_IdAndSpot_Id(any(), any());
    }

    private TouristSpot newSpot(Long id) {
        TouristSpot spot = TouristSpot.builder()
                .tourApiContentId("TEST-SPOT-" + id)
                .name("테스트 장소 " + id)
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.1))
                .longitude(BigDecimal.valueOf(129.1))
                .category(Category.CAFE)
                .build();
        setId(spot, id);
        return spot;
    }

    private void setId(TouristSpot spot, Long id) {
        try {
            var field = TouristSpot.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(spot, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
