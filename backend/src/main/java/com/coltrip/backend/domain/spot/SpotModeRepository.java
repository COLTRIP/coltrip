package com.coltrip.backend.domain.spot;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotModeRepository extends JpaRepository<SpotMode, Long> {
    List<SpotMode> findBySpot_Id(Long spotId);
}
