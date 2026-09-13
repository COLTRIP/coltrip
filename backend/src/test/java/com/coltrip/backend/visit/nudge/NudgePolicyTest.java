package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NudgePolicyTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);
    private final NudgePolicy policy = new NudgePolicy(Duration.ofHours(5), Duration.ofMinutes(10));

    @Test void absoluteBoundary() {
        assertEquals("TRIGGERED", evaluate(39, 39));
        assertEquals("NOT_TRIGGERED", evaluate(40, 40));
    }
    @Test void dropBoundary() {
        assertEquals("NOT_TRIGGERED", evaluate(70, 56));
        assertEquals("TRIGGERED", evaluate(70, 55));
    }
    @Test void nullPolicy() {
        assertEquals("SCORE_UNAVAILABLE", evaluate(70, null));
        assertEquals("TRIGGERED", evaluate(null, 39));
        assertEquals("BASELINE_UNAVAILABLE", evaluate(null, 40));
    }
    @Test void staleAndFutureCurrent() {
        assertEquals("SCORE_STALE", policy.evaluate(70, now, now, 39, now.minusHours(5).minusSeconds(1), now));
        assertEquals("TRIGGERED", policy.evaluate(70, now, now, 39, now.minusHours(5), now));
        assertEquals("SCORE_STALE", policy.evaluate(70, now, now, 39, now.plusSeconds(1), now));
        assertEquals("SCORE_STALE", policy.evaluate(70, now, now, 39, null, now));
    }
    @Test void baselineIsValidatedAtStartNotAtPresent() {
        assertEquals("TRIGGERED", policy.evaluate(70, now.minusHours(20), now.minusHours(19), 55, now, now));
        assertEquals("BASELINE_UNAVAILABLE", policy.evaluate(70, now.minusHours(10), now.minusHours(1), 55, now, now));
        assertEquals("BASELINE_UNAVAILABLE", policy.evaluate(70, null, now, 55, now, now));
    }
    @Test void invalidScoresAreNotZero() {
        assertEquals("SCORE_UNAVAILABLE", evaluate(70, -1));
        assertEquals("SCORE_UNAVAILABLE", evaluate(70, 101));
        assertEquals("BASELINE_UNAVAILABLE", evaluate(101, 55));
    }
    private String evaluate(Integer start, Integer current) {
        return policy.evaluate(start, now.minusHours(1), now.minusHours(1), current, now, now);
    }
}
