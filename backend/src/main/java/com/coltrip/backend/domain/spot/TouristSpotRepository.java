package com.coltrip.backend.domain.spot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findByTourApiContentId(String tourApiContentId);

    // mode는 EXISTS로 "해당 모드를 가진 장소"만 거르고, 응답에 담을 모드 목록은 fetch join으로 전부 가져온다
    // (mode로 필터링했다고 해서 그 모드 하나만 응답에 담기면 안 되므로 조건절과 fetch를 분리)
    @Query("""
            SELECT DISTINCT s FROM TouristSpot s
            LEFT JOIN FETCH s.spotModes
            WHERE s.latitude BETWEEN :swLat AND :neLat
              AND s.longitude BETWEEN :swLng AND :neLng
              AND (:category IS NULL OR s.category = :category)
              AND (:mode IS NULL OR EXISTS (
                    SELECT 1 FROM SpotMode sm WHERE sm.spot = s AND sm.mode = :mode))
            """)
    List<TouristSpot> findInBounds(@Param("swLat") BigDecimal swLat,
                                   @Param("neLat") BigDecimal neLat,
                                   @Param("swLng") BigDecimal swLng,
                                   @Param("neLng") BigDecimal neLng,
                                   @Param("category") Category category,
                                   @Param("mode") Mode mode);

    @Query("""
            SELECT s FROM TouristSpot s
            LEFT JOIN FETCH s.spotModes
            WHERE s.id = :spotId
            """)
    Optional<TouristSpot> findByIdWithModes(@Param("spotId") Long spotId);
}
