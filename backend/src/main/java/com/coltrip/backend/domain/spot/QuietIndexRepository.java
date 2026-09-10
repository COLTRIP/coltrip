package com.coltrip.backend.domain.spot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuietIndexRepository extends JpaRepository<QuietIndex, Long> {

    @Query("""
            SELECT q FROM QuietIndex q
            WHERE q.spot.id = :spotId AND q.calculatedAt >= :since
            ORDER BY q.calculatedAt ASC
            """)
    List<QuietIndex> findBySpotIdSince(@Param("spotId") Long spotId, @Param("since") LocalDateTime since);

    Optional<QuietIndex> findBySpot_IdAndCalculatedAt(Long spotId, LocalDateTime calculatedAt);
}
