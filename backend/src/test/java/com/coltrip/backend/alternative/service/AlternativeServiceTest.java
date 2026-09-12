package com.coltrip.backend.alternative.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.alternative.client.AiAlternativeClient;
import com.coltrip.backend.alternative.client.AiAlternativeClient.Candidate;
import com.coltrip.backend.alternative.client.AiAlternativeClient.Result;
import com.coltrip.backend.alternative.dto.AlternativeListResponse.Place;
import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.alternative.service.AlternativeDataReader.Target;
import com.coltrip.backend.common.util.GeoUtils;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AlternativeServiceTest {

    @Mock AlternativeDataReader reader;
    @Mock AiAlternativeClient client;
    private AlternativeService service;
    private final Target target = new Target(1L, "origin", bd(35), bd(129), 60.0);

    @BeforeEach
    void setUp() {
        service = new AlternativeService(reader, client,
                Clock.fixed(Instant.parse("2026-09-12T04:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void usesKoreanHourWeekendAndVisitBaseline() {
        when(reader.loadTarget(7L, 1L)).thenReturn(target);
        when(client.fetch(any())).thenReturn(new Result(false, 80.0, List.of()));
        var response = service.find(7L, 1L);
        var captor = ArgumentCaptor.forClass(AiAlternativeClient.Request.class);
        verify(client).fetch(captor.capture());
        assertEquals(13, captor.getValue().hour());
        assertTrue(captor.getValue().isWeekend());
        assertEquals(60.0, captor.getValue().baselineQuietIndex());
        assertFalse(response.triggered());
        verify(reader, never()).loadPlaces(any(), anyList());
    }

    @Test
    void filtersOriginalDuplicateUnknownFarAndMoreCrowdedPlaces() {
        when(reader.loadTarget(null, 1L)).thenReturn(target);
        when(client.fetch(any())).thenReturn(new Result(true, 30.0, List.of(
                candidate("origin", 90, .9, 0), candidate("near", 70.3, .7, 1),
                candidate("near", 70.3, .9, 1), candidate("unknown", 90, .8, 1),
                candidate("far", 90, .8, 1), candidate("crowded", 20, .8, 1),
                candidate("ai-far", 90, .8, 3.01))));
        when(reader.loadPlaces(isNull(), anyList())).thenReturn(Map.of(
                "origin", place(1, 35), "near", place(2, 35.01),
                "far", place(3, 35.1), "crowded", place(4, 35.01),
                "ai-far", place(5, 35.01)));

        var response = service.find(null, 1L);

        assertEquals(1, response.alternatives().size());
        var item = response.alternatives().getFirst();
        assertEquals(2L, item.spot().id());
        assertEquals(.9, item.score());
        assertEquals(70.3, item.quietIndex());
        assertEquals(1.11, item.distanceKm());
    }

    @Test
    void filtersUsingUnroundedDistanceAtThreeKilometers() {
        when(reader.loadTarget(null, 1L)).thenReturn(target);
        double boundary = 35 + Math.toDegrees(3000.0 / 6371000);
        assertTrue(GeoUtils.distanceMeters(bd(35), bd(129), bd(boundary - .0000001), bd(129)) < 3000);
        assertTrue(GeoUtils.distanceMeters(bd(35), bd(129), bd(boundary + .0000001), bd(129)) > 3000);
        when(client.fetch(any())).thenReturn(new Result(true, 30.0,
                List.of(candidate("inside", 70, .8, 3), candidate("outside", 80, .9, 3))));
        when(reader.loadPlaces(isNull(), anyList())).thenReturn(Map.of(
                "inside", place(2, boundary - .0000001), "outside", place(3, boundary + .0000001)));
        var response = service.find(null, 1L);
        assertEquals(2L, response.alternatives().getFirst().spot().id());
        assertEquals(1, response.alternatives().size());
    }

    @Test
    void triggeredCanBeTrueWithNoDisplayableAlternative() {
        when(reader.loadTarget(null, 1L)).thenReturn(target);
        when(client.fetch(any())).thenReturn(new Result(true, 30.0, List.of(candidate("unknown", 80, .8, 1))));
        when(reader.loadPlaces(isNull(), anyList())).thenReturn(Map.of());
        var response = service.find(null, 1L);
        assertTrue(response.triggered());
        assertTrue(response.alternatives().isEmpty());
        assertFalse(response.message().isBlank());
    }

    @Test
    void missingSpotDoesNotCallAi() {
        when(reader.loadTarget(null, 999L)).thenThrow(new SpotNotFoundException());
        assertThrows(SpotNotFoundException.class, () -> service.find(null, 999L));
        verifyNoInteractions(client);
    }

    @Test
    void propagatesAiFailureInsteadOfReturningEmptySuccess() {
        when(reader.loadTarget(null, 1L)).thenReturn(target);
        when(client.fetch(any())).thenThrow(new AiIntegrationException(HttpStatus.BAD_GATEWAY, "AiRequestFailed", "failed"));
        assertThrows(AiIntegrationException.class, () -> service.find(null, 1L));
    }

    private Candidate candidate(String id, double quiet, double score, double distance) {
        return new Candidate(id, id, quiet, distance, score, "Quieter");
    }

    private Place place(long id, double lat) {
        return new Place(id, "Place " + id, "Busan", "PARK", List.of("WALK"),
                null, bd(lat), bd(129), false);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
