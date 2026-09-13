package com.coltrip.backend.domain.like;

import com.coltrip.backend.domain.spot.TouristSpot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotLikeRepository extends JpaRepository<SpotLike, Long> {

    Optional<SpotLike> findByUser_IdAndSpot_Id(Long userId, Long spotId);

    boolean existsByUser_IdAndSpot_Id(Long userId, Long spotId);

    long countByUser_Id(Long userId);

    // 목록 조회 시 장소별로 반복 쿼리하지 않고 한 번에 좋아요 여부를 확인하기 위한 일괄 조회
    @Query("""
            SELECT l.spot.id FROM SpotLike l
            WHERE l.user.id = :userId AND l.spot.id IN :spotIds
            """)
    Set<Long> findLikedSpotIds(@Param("userId") Long userId, @Param("spotIds") Collection<Long> spotIds);

    @Query("""
            SELECT l.spot FROM SpotLike l
            LEFT JOIN FETCH l.spot.spotModes
            WHERE l.user.id = :userId
            ORDER BY l.createdAt DESC
            """)
    List<TouristSpot> findLikedSpotsByUserId(@Param("userId") Long userId);

    void deleteByUser_Id(Long userId);
}
