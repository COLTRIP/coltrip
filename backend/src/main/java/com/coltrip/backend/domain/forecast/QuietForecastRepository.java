package com.coltrip.backend.domain.forecast;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuietForecastRepository extends JpaRepository<QuietForecast, Long> {
    Optional<QuietForecast> findBySpot_IdAndTargetAtAndSourceAndGeneratedAt(
            Long spotId, LocalDateTime targetAt, String source, LocalDateTime generatedAt);

    // 최신 버전이 만료돼도 과거 버전으로 되돌아가지 않도록 하위 쿼리에는 만료 조건을 넣지 않는다.
    @Query("""
            select distinct f from QuietForecast f
            join fetch f.spot s left join fetch s.spotModes
            where f.targetAt = :target and f.source = :source
              and f.generatedAt <= :now and f.validUntil > :now
              and s.latitude between :south and :north
              and s.longitude between :west and :east
              and (:category is null or s.category = :category)
              and (:mode is null or exists (select 1 from SpotMode sm where sm.spot = s and sm.mode = :mode))
              and not exists (select 1 from QuietForecast newer
                  where newer.spot = s and newer.targetAt = f.targetAt and newer.source = f.source
                    and newer.generatedAt <= :now and newer.generatedAt > f.generatedAt)
            """)
    List<QuietForecast> findCandidates(@Param("target") LocalDateTime target,
            @Param("source") String source, @Param("now") LocalDateTime now,
            @Param("south") BigDecimal south, @Param("north") BigDecimal north,
            @Param("west") BigDecimal west, @Param("east") BigDecimal east,
            @Param("category") Category category, @Param("mode") Mode mode);

    @Query("""
            select f from QuietForecast f
            where f.spot.id = :spotId and f.targetAt >= :start and f.targetAt < :end
              and f.source = :source and f.generatedAt <= :now and f.validUntil > :now
              and not exists (select 1 from QuietForecast newer
                  where newer.spot = f.spot and newer.targetAt = f.targetAt and newer.source = f.source
                    and newer.generatedAt <= :now and newer.generatedAt > f.generatedAt)
            order by f.targetAt
            """)
    List<QuietForecast> findTimeline(@Param("spotId") Long spotId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            @Param("source") String source, @Param("now") LocalDateTime now);
}
