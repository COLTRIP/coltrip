package com.coltrip.backend.forecast;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test void validatesLimitRange() {
        assertThrows(InvalidForecastRequestException.class, () -> policy.validateLimit(0));
        assertThrows(InvalidForecastRequestException.class, () -> policy.validateLimit(51));
    }

    @Test void inputOffsetNormalizesToKoreanTime() {
        assertEquals(now.withMinute(0), policy.local(OffsetDateTime.parse("2026-09-13T03:00:00Z")));
    }
}
