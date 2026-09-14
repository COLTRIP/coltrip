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
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
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

    @ParameterizedTest
    @CsvSource({
            "2026-09-11T15:05:00Z, true",
            "2026-09-13T15:05:00Z, false"
    })
    void determinesWeekendAtKoreanMidnight(String instant, boolean expectedWeekend) {
        Clock boundary = Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"));
        var scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, boundary);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of("126081"));
        when(client.fetchMap(0, expectedWeekend)).thenReturn(List.of());

        scheduler.sync();

        verify(client).fetchMap(0, expectedWeekend);
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
    void skipsMalformedAndDuplicateItemsAndContinuesAfterSaveFailure(CapturedOutput output) {
        var scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, clock);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of("dup", "removed", "failed", "good"));
        when(client.fetchMap(14, true)).thenReturn(Arrays.asList(
                null, new Item(null, null, null, 50.0), new Item(" ", null, null, 50.0),
                new Item("dup", null, null, 20.0), new Item("dup", null, null, 90.0),
                new Item("unknown", null, null, 50.0), new Item("removed", null, null, 50.0),
                new Item("failed", null, null, 50.0), new Item("good", null, null, 100.0)));
        when(applier.apply(eq("removed"), eq(50.0), any())).thenReturn(false);
        when(applier.apply(eq("failed"), eq(50.0), any())).thenThrow(new IllegalStateException("private detail"));
        when(applier.apply(eq("good"), eq(100.0), any())).thenReturn(true);

        scheduler.sync();

        verify(applier).apply(eq("removed"), eq(50.0), any());
        verify(applier).apply(eq("failed"), eq(50.0), any());
        verify(applier).apply(eq("good"), eq(100.0), any());
        org.mockito.Mockito.verifyNoMoreInteractions(applier);
        org.junit.jupiter.api.Assertions.assertTrue(output.getOut().contains("응답 9건, 성공 1건, 제외 7건, 실패 1건"));
        org.junit.jupiter.api.Assertions.assertFalse(output.getAll().contains("private detail"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 100.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void skipsInvalidScoresBeforePersistence(double score) {
        var scheduler = new QuietIndexMapSyncScheduler(client, spots, applier, clock);
        when(spots.findAllTourApiContentIds()).thenReturn(List.of("126081"));
        when(client.fetchMap(14, true)).thenReturn(List.of(new Item("126081", null, null, score)));
        scheduler.sync();
        org.mockito.Mockito.verifyNoInteractions(applier);
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
