package com.coltrip.backend.domain.like;

import com.coltrip.backend.domain.spot.TouristSpot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotLikeRepository extends JpaRepository<SpotLike, Long> {

    Optional<SpotLike> findByUser_IdAndSpot_Id(Long userId, Long spotId);

    boolean existsByUser_IdAndSpot_Id(Long userId, Long spotId);

    @Query("""
            SELECT l.spot FROM SpotLike l
            LEFT JOIN FETCH l.spot.spotModes
            WHERE l.user.id = :userId
            ORDER BY l.createdAt DESC
            """)
    List<TouristSpot> findLikedSpotsByUserId(@Param("userId") Long userId);

    void deleteByUser_Id(Long userId);
}
