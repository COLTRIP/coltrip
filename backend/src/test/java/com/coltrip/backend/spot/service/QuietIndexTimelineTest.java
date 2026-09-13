package com.coltrip.backend.spot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.QuietIndex;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.spot.dto.QuietIndexTimelineResponse;
import com.coltrip.backend.spot.dto.QuietIndexTimelineResponse.TimelineSlot;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 고요지수 24시간 타임라인(#38) 검증
@ExtendWith(MockitoExtension.class)
class QuietIndexTimelineTest {

    private static final Long SPOT_ID = 1L;

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private QuietIndexRepository quietIndexRepository;

    @InjectMocks
    private SpotService spotService;

    @Test
    void returns24SlotsWithLatestValuePerHourAndNullForMissingSlots() {
        LocalDateTime currentSlotStart = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        TouristSpot spot = newSpot();

        // 2시간 전 슬롯에 관측값 두 건(늦게 온 값이 대표값이 되어야 함)
        LocalDateTime twoHoursAgoSlot = currentSlotStart.minusHours(2);
        QuietIndex earlier = QuietIndex.builder().spot(spot).quietScore(80)
                .calculatedAt(twoHoursAgoSlot.plusMinutes(5)).build();
        QuietIndex later = QuietIndex.builder().spot(spot).quietScore(40)
                .calculatedAt(twoHoursAgoSlot.plusMinutes(50)).build();

        // 5시간 전 슬롯에 관측값 한 건
        LocalDateTime fiveHoursAgoSlot = currentSlotStart.minusHours(5);
        QuietIndex single = QuietIndex.builder().spot(spot).quietScore(60)
                .calculatedAt(fiveHoursAgoSlot.plusMinutes(30)).build();

        when(touristSpotRepository.existsById(SPOT_ID)).thenReturn(true);
        when(quietIndexRepository.findBySpotIdSince(eq(SPOT_ID), any()))
                .thenReturn(List.of(single, earlier, later));

        QuietIndexTimelineResponse response = spotService.getQuietIndexTimeline(SPOT_ID);

        assertEquals(24, response.timeline().size());
        assertEquals(currentSlotStart.minusHours(23), response.timeline().get(0).slotStartAt());
        assertEquals(currentSlotStart, response.timeline().get(23).slotStartAt());

        TimelineSlot twoHoursAgoResult = findSlot(response, twoHoursAgoSlot);
        assertEquals(40, twoHoursAgoResult.quietScore());
        assertEquals(later.getCalculatedAt(), twoHoursAgoResult.observedAt());

        TimelineSlot fiveHoursAgoResult = findSlot(response, fiveHoursAgoSlot);
        assertEquals(60, fiveHoursAgoResult.quietScore());

        TimelineSlot emptySlot = findSlot(response, currentSlotStart.minusHours(10));
        assertNull(emptySlot.quietScore());
        assertNull(emptySlot.observedAt());
    }

    @Test
    void throwsWhenSpotDoesNotExist() {
        when(touristSpotRepository.existsById(SPOT_ID)).thenReturn(false);

        assertThrows(SpotNotFoundException.class, () -> spotService.getQuietIndexTimeline(SPOT_ID));
        verify(quietIndexRepository, never()).findBySpotIdSince(any(), any());
    }

    private TimelineSlot findSlot(QuietIndexTimelineResponse response, LocalDateTime slotStartAt) {
        return response.timeline().stream()
                .filter(slot -> slot.slotStartAt().equals(slotStartAt))
                .findFirst()
                .orElseThrow();
    }

    private TouristSpot newSpot() {
        return TouristSpot.builder()
                .tourApiContentId("TEST-SPOT")
                .name("테스트 장소")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.1))
                .longitude(BigDecimal.valueOf(129.1))
                .category(Category.CAFE)
                .build();
    }
}
