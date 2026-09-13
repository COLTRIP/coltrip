package com.coltrip.backend.quietindex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.quietindex.client.AiQuietIndexMapClient;
import com.coltrip.backend.quietindex.client.AiQuietIndexMapClient.Item;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QuietIndexMapSyncSchedulerTest {

    @Mock
    private AiQuietIndexMapClient client;

    @Mock
    private TouristSpotRepository spots;

    @Mock
    private QuietIndexMapApplier applier;

    // 2026-09-13T14:00:00+09:00, 일요일(주말)
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void skipsAiCallWhenNoLocalSpotsExist() {
        QuietIndexMapSyncScheduler scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, clock);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of());

        scheduler.sync();

        verify(client, never()).fetchMap(anyInt(), anyBoolean());
    }

    @Test
    void callsAiWithKoreanHourAndWeekendThenAppliesOnlyKnownNonNullItems() {
        QuietIndexMapSyncScheduler scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, clock);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of("126081", "126148"));
        when(client.fetchMap(14, true)).thenReturn(List.of(
                new Item("126081", "해운대해수욕장", 100, 85.4),
                new Item("999999", "우리 DB에 없는 장소", 10, 20.0),
                new Item("126148", "범어사", 5, null)));
        when(applier.apply(eq("126081"), eq(85.4), any())).thenReturn(true);

        scheduler.sync();

        verify(applier, times(1)).apply(eq("126081"), eq(85.4), any());
        verify(applier, never()).apply(eq("999999"), any(Double.class), any());
        verify(applier, never()).apply(eq("126148"), any(Double.class), any());
    }

    @Test
    void aiFailureIsSwallowedSoLastKnownGoodValuesStay() {
        QuietIndexMapSyncScheduler scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, clock);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of("126081"));
        when(client.fetchMap(anyInt(), anyBoolean()))
                .thenThrow(new AiIntegrationException(HttpStatus.BAD_GATEWAY, "AiRequestFailed", "실패"));

        scheduler.sync();

        verify(applier, never()).apply(any(), any(Double.class), any());
    }
}
