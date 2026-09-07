package com.coltrip.backend.domain.visit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    boolean existsByUser_IdAndStatus(Long userId, VisitStatus status);

    long countByUser_IdAndStatus(Long userId, VisitStatus status);

    void deleteByUser_Id(Long userId);
}
