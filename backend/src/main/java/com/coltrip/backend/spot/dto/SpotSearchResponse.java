package com.coltrip.backend.spot.dto;

import java.util.List;

public record SpotSearchResponse(List<SpotSummaryResponse> spots, int page, int size,
                                 long totalElements, int totalPages, boolean hasNext) {
}
