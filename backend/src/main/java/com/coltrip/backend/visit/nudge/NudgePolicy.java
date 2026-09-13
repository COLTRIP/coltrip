package com.coltrip.backend.visit.nudge;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NudgePolicy {
    private final Duration maxScoreAge;
    private final Duration proposalTtl;

    public NudgePolicy(@Value("${visit.nudge.max-score-age:PT5H}") Duration maxScoreAge,
                       @Value("${visit.nudge.proposal-ttl:PT10M}") Duration proposalTtl) {
        if (maxScoreAge.isNegative() || maxScoreAge.isZero()
                || proposalTtl.isNegative() || proposalTtl.isZero()) {
            throw new IllegalArgumentException("Nudge durations must be positive");
        }
        this.maxScoreAge = maxScoreAge;
        this.proposalTtl = proposalTtl;
    }

    public String evaluate(Integer start, LocalDateTime startObservedAt, LocalDateTime startedAt,
                           Integer current, LocalDateTime observedAt, LocalDateTime now) {
        if (!valid(current)) {
            return "SCORE_UNAVAILABLE";
        }
        if (!fresh(observedAt, now)) {
            return "SCORE_STALE";
        }
        if (current < 40) {
            return "TRIGGERED";
        }
        if (!valid(start) || !fresh(startObservedAt, startedAt)) {
            return "BASELINE_UNAVAILABLE";
        }
        return start - current >= 15 ? "TRIGGERED" : "NOT_TRIGGERED";
    }

    public LocalDateTime expiresAt(LocalDateTime issuedAt) {
        return issuedAt.plus(proposalTtl);
    }

    private boolean valid(Integer score) {
        return score != null && score >= 0 && score <= 100;
    }

    private boolean fresh(LocalDateTime observedAt, LocalDateTime reference) {
        return observedAt != null && reference != null && !observedAt.isAfter(reference)
                && !observedAt.isBefore(reference.minus(maxScoreAge));
    }
}
