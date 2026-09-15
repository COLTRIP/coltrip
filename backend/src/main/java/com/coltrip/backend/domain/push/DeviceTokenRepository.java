package com.coltrip.backend.domain.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);

    void deleteByUser_IdAndToken(Long userId, String token);
}
