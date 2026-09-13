package com.coltrip.backend.forecast;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;

class ForecastPolicyTest {
    private final ForecastPolicy policy = new ForecastPolicy();
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 30);

    @Test void validatesHourlyWindow() {
        assertEquals(now.withMinute(0), policy.target(now.toLocalDate(), 12, now));
        assertEquals(now.withMinute(0).plusDays(7), policy.target(now.plusDays(7).toLocalDate(), 12, now));
        assertThrows(InvalidForecastRequestException.class, () -> policy.target(now.toLocalDate(), 11, now));
        assertThrows(InvalidForecastRequestException.class, () -> policy.target(now.toLocalDate(), 24, now));
        assertThrows(InvalidForecastRequestException.class, () -> policy.target(now.plusDays(7).toLocalDate(), 13, now));
    }

    @Test void defaultCenterAndCoordinateValidation() {
        var area = policy.area(null, null, 15000, 20);
        assertTrue(area.defaultCenter());
        assertEquals(new BigDecimal("35.1796"), area.latitude());
        assertThrows(InvalidForecastRequestException.class, () -> policy.area(BigDecimal.ZERO, null, 15000, 20));
        assertThrows(InvalidForecastRequestException.class, () -> policy.area(new BigDecimal("91"), BigDecimal.ZERO, 15000, 20));
        assertThrows(InvalidForecastRequestException.class, () -> policy.area(null, null, 0, 20));
        assertThrows(InvalidForecastRequestException.class, () -> policy.area(null, null, 15000, 51));
    }

    @Test void handlesDateLineAndPoleWithoutLosingCandidates() {
        var dateLine = policy.area(BigDecimal.ZERO, new BigDecimal("179.99"), 15000, 20);
        assertEquals(-180.0, dateLine.west().doubleValue());
        assertEquals(180.0, dateLine.east().doubleValue());
        var pole = policy.area(new BigDecimal("90"), BigDecimal.ZERO, 15000, 20);
        assertEquals(90.0, pole.north().doubleValue());
    }

    @Test void inputOffsetNormalizesToKoreanTime() {
        assertEquals(now.withMinute(0), policy.local(OffsetDateTime.parse("2026-09-13T03:00:00Z")));
    }
}
