package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.alternative.dto.AlternativeListResponse.Alternative;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record NudgeResponse(String state, Proposal proposal) {
    public static NudgeResponse state(String state) {
        return new NudgeResponse(state, null);
    }

    public record Proposal(
            @Schema(description = "방문당 하나인 제안 ID. visitId와 같으며 프론트 중복 표시 방지 키") Long suggestionId,
            Long sourceSpotId,
            Integer startQuietScore,
            Integer currentQuietScore,
            LocalDateTime sourceObservedAt,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            List<Alternative> alternatives) {
    }
}
