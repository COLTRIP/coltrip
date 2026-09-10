package com.coltrip.backend.domain.visit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    boolean existsByUser_IdAndStatus(Long userId, VisitStatus status);

    long countByUser_IdAndStatus(Long userId, VisitStatus status);

    @Query("""
            SELECT v FROM Visit v
            JOIN FETCH v.spot
            WHERE v.user.id = :userId
              AND v.status = :status
            ORDER BY v.startedAt DESC
            """)
    List<Visit> findByUserIdAndStatusWithSpot(@Param("userId") Long userId,
                                              @Param("status") VisitStatus status);

    @Query("""
            SELECT v FROM Visit v
            JOIN FETCH v.spot
            WHERE v.user.id = :userId
              AND v.status = :status
            ORDER BY v.completedAt DESC
            """)
    List<Visit> findByUserIdAndStatusOrderByCompletedAtDesc(@Param("userId") Long userId,
                                                             @Param("status") VisitStatus status);

    void deleteByUser_Id(Long userId);
}