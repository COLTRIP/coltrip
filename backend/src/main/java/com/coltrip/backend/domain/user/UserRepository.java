package com.coltrip.backend.domain.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByGoogleSub(String googleSub);

    // 방문 시작 시 동시 요청으로 STARTED 방문이 중복 생성되는 것을 막기 위한 사용자별 행 잠금.
    // 같은 사용자의 동시 방문 시작 요청을 이 잠금으로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
