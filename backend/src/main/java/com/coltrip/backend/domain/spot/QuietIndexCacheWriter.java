package com.coltrip.backend.domain.spot;

import java.time.LocalDateTime;

// AI push(#36)와 AI 지도 API 풀링(#63)이 같은 이력 upsert·캐시 갱신 규칙을 공유하기 위한 헬퍼.
public final class QuietIndexCacheWriter {

    private QuietIndexCacheWriter() {
    }

    public static void upsert(QuietIndexRepository repository, TouristSpot spot,
                              int quietScore, LocalDateTime calculatedAt, String rawMetrics) {
        repository.findBySpot_IdAndCalculatedAt(spot.getId(), calculatedAt)
                .ifPresentOrElse(
                        existing -> existing.correct(quietScore, rawMetrics),
                        () -> repository.save(QuietIndex.builder()
                                .spot(spot)
                                .quietScore(quietScore)
                                .rawMetrics(rawMetrics)
                                .calculatedAt(calculatedAt)
                                .build()));
        spot.updateQuietScoreIfNewer(quietScore, calculatedAt);
    }
}
