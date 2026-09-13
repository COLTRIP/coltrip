package com.coltrip.backend.quietindex.service;

import com.coltrip.backend.domain.spot.QuietIndexCacheWriter;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 스케줄러(AI 호출 이후)에서 장소 1건씩 잠금 후 캐시를 갱신한다. 트랜잭션을 짧게 유지하기 위해
// AI 응답 대기와 분리된 별도 빈으로 둔다(같은 클래스 내 호출로는 @Transactional 프록시가 적용되지 않음).
@Component
@RequiredArgsConstructor
public class QuietIndexMapApplier {

    private final TouristSpotRepository spots;
    private final QuietIndexRepository quietIndexRepository;

    @Transactional
    public boolean apply(String poiId, double quietIndex, LocalDateTime calculatedAt) {
        if (!StringUtils.hasText(poiId) || !Double.isFinite(quietIndex)
                || quietIndex < 0 || quietIndex > 100 || calculatedAt == null) {
            throw new IllegalArgumentException("Invalid quiet index map item");
        }
        return spots.findByTourApiContentIdForUpdate(poiId)
                .map(spot -> {
                    QuietIndexCacheWriter.upsert(quietIndexRepository, spot,
                            (int) Math.round(quietIndex), calculatedAt, null);
                    return true;
                })
                .orElse(false);
    }
}
