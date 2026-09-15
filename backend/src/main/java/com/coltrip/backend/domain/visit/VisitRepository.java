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

    // 백그라운드 평가 대상 후보. 알림을 끈 사용자는 NudgeStore에서도 걸러지지만
    // 여기서 먼저 제외해 불필요한 AI 호출 자체를 만들지 않는다.
    @Query("""
            SELECT v FROM Visit v JOIN FETCH v.user
            WHERE v.status = :status AND v.user.alternativeNotificationEnabled = true
            """)
    List<Visit> findForBackgroundNudge(@Param("status") VisitStatus status);
}