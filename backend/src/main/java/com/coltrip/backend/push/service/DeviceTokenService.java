package com.coltrip.backend.push.service;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.push.DeviceToken;
import com.coltrip.backend.domain.push.DeviceTokenRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokens;
    private final UserRepository users;

    // 같은 토큰으로 다른 계정이 다시 등록하면(기기 재사용/재로그인) 소유자를 옮긴다.
    public void register(Long userId, String token) {
        User user = users.findById(userId).orElseThrow(UnauthorizedException::new);
        deviceTokens.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.reassignOwner(user),
                        () -> deviceTokens.save(DeviceToken.builder().user(user).token(token).build()));
    }

    // 소유하지 않은 토큰이거나 이미 없는 토큰이면 조용히 무시한다(멱등).
    public void unregister(Long userId, String token) {
        deviceTokens.deleteByUser_IdAndToken(userId, token);
    }
}
