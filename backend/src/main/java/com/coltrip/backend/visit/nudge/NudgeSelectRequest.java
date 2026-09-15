package com.coltrip.backend.visit.nudge;

import jakarta.validation.constraints.NotNull;

public record NudgeSelectRequest(
        @NotNull Long spotId) {
}
