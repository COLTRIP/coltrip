package com.coltrip.backend.domain.review;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByVisit_Id(Long visitId);

    // 방문 이력 목록에서 각 방문의 리뷰 작성 여부/reviewId를 일괄 조회하기 위한 용도
    List<Review> findByVisit_IdIn(Collection<Long> visitIds);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.spot.id = :spotId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findBySpotIdWithUser(@Param("spotId") Long spotId);

    void deleteByUser_Id(Long userId);
}
