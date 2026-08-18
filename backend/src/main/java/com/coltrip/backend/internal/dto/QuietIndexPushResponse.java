package com.coltrip.backend.internal.dto;

public record QuietIndexPushResponse(
        Long spotId,
        Integer quietScore,
        String quietLevel
) {
}
