package com.coltrip.backend.spot.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuietIndexTimelineResponse(
        List<TimelineSlot> timeline
) {
    public record TimelineSlot(
            LocalDateTime slotStartAt,
            Integer quietScore,
            LocalDateTime observedAt
    ) {
    }
}
