package com.coltrip.backend.domain.spot;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuietIndexRepository extends JpaRepository<QuietIndex, Long> {

    Optional<QuietIndex> findBySpot_IdAndCalculatedAt(Long spotId, LocalDateTime calculatedAt);
}
