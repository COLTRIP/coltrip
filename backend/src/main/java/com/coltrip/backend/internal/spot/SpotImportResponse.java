package com.coltrip.backend.internal.spot;

import java.time.OffsetDateTime;
import java.util.List;

public record SpotImportResponse(int applied, int ignoredStale, List<Result> spots) {
    public record Result(String tourApiContentId, Long spotId, String status, OffsetDateTime sourceUpdatedAt) { }
}
