package com.coltrip.backend.domain.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByVisit_Id(Long visitId);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.spot.id = :spotId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findBySpotIdWithUser(@Param("spotId") Long spotId);

    void deleteByUser_Id(Long userId);
}
