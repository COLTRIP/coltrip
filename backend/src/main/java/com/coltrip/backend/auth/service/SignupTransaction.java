package com.coltrip.backend.auth.service;

import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.exception.AlreadyRegisteredUserException;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.service.UserStatsReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupTransaction {
    private final UserRepository users;
    private final JwtProvider jwt;
    private final UserStatsReader stats;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JwtTokenResponse register(GoogleUserInfo info) {
        if (users.existsByGoogleSub(info.sub())) {
            throw new AlreadyRegisteredUserException();
        }
        User user = users.saveAndFlush(User.builder().googleSub(info.sub()).email(info.email()).build());
        String accessToken = jwt.createAccessToken(user.getId());
        String refreshToken = jwt.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);
        return JwtTokenResponse.of(accessToken, refreshToken, true, stats.toResponse(user));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isRegistered(String googleSub) {
        return users.existsByGoogleSub(googleSub);
    }
}
