package com.coltrip.backend.domain.spot;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findByTourApiContentId(String tourApiContentId);
}
