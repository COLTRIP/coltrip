package com.coltrip.backend.domain.spot;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    @Query(value = "SELECT s.id FROM TouristSpot s WHERE LOWER(s.name) LIKE LOWER(:pattern) ESCAPE '!' ORDER BY s.name, s.id",
            countQuery = "SELECT COUNT(s) FROM TouristSpot s WHERE LOWER(s.name) LIKE LOWER(:pattern) ESCAPE '!'")
    Page<Long> searchIdsByName(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT DISTINCT s FROM TouristSpot s LEFT JOIN FETCH s.spotModes WHERE s.id IN :ids")
    List<TouristSpot> findByIdsWithModes(@Param("ids") List<Long> ids);

    Optional<TouristSpot> findByTourApiContentId(String tourApiContentId);

    // AI 지도 API 응답(약 594개) 중 우리 DB에 있는 장소만 걸러내기 위한 존재 목록
    @Query("SELECT s.tourApiContentId FROM TouristSpot s")
    List<String> findAllTourApiContentIds();

    // 고요지수 push 시 같은 장소에 대한 동시 요청을 직렬화해 캐시 갱신이 최신 값을 잃지 않도록 한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TouristSpot s WHERE s.tourApiContentId = :tourApiContentId")
    Optional<TouristSpot> findByTourApiContentIdForUpdate(@Param("tourApiContentId") String tourApiContentId);

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

    @Query("""
            SELECT DISTINCT s FROM TouristSpot s
            LEFT JOIN FETCH s.spotModes
            WHERE s.tourApiContentId IN :contentIds
            """)
    List<TouristSpot> findByTourApiContentIdsWithModes(@Param("contentIds") List<String> contentIds);

    // 존재하지 않는 장소의 동시 최초 요청도 유니크 키로 직렬화한다. 기존 행은 여기서 변경하지 않는다.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO tourist_spot
                (tour_api_content_id, name, address, latitude, longitude, category,
                 description, image_url, recommend_reason, created_at, updated_at)
            VALUES (:contentId, :name, :address, :latitude, :longitude, :category,
                    :description, :imageUrl, :recommendReason, :now, :now)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int ensureImportRow(@Param("contentId") String contentId,
                        @Param("name") String name, @Param("address") String address,
                        @Param("latitude") BigDecimal latitude, @Param("longitude") BigDecimal longitude,
                        @Param("category") String category, @Param("description") String description,
                        @Param("imageUrl") String imageUrl, @Param("recommendReason") String recommendReason,
                        @Param("now") LocalDateTime now);
}
