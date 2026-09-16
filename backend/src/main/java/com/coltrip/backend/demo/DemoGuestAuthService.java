package com.coltrip.backend.demo;

import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.service.UserStatsReader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 시연 심사용 - 구글 인증 없이 매 호출마다 새 게스트 계정을 만들고 즉시 토큰을 발급한다.
// demo.enabled=true(단독 demo 프로필)에서만 빈이 등록되며, 운영 환경에는 이 엔드포인트 자체가 존재하지 않는다.
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true")
public class DemoGuestAuthService {
    private final UserRepository users;
    private final JwtProvider jwtProvider;
    private final UserStatsReader userStatsReader;

    @Transactional
    public JwtTokenResponse createGuestSession() {
        String sub = DemoAccessPolicy.GUEST_SUB_PREFIX + UUID.randomUUID();
        User user = users.saveAndFlush(User.builder().googleSub(sub).email(sub + "@coltrip.demo").build());
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);
        return JwtTokenResponse.of(accessToken, refreshToken, true, userStatsReader.toResponse(user));
    }
}
