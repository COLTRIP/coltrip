package com.coltrip.backend.auth.service;

import com.coltrip.backend.auth.dto.AuthIntent;
import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.exception.AlreadyRegisteredUserException;
import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.auth.exception.UserNotRegisteredException;
import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.service.UserStatsReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtProvider jwtProvider;
    private final UserStatsReader userStatsReader;

    public JwtTokenResponse googleLogin(String idToken, AuthIntent intent) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(idToken);

        return switch (intent) {
            case LOGIN -> login(googleUserInfo);
            case SIGNUP -> signup(googleUserInfo);
        };
    }

    private JwtTokenResponse login(GoogleUserInfo googleUserInfo) {
        User user = userRepository.findByGoogleSub(googleUserInfo.sub())
                .orElseThrow(UserNotRegisteredException::new);
        return issueTokens(user, false);
    }

    private JwtTokenResponse signup(GoogleUserInfo googleUserInfo) {
        if (userRepository.existsByGoogleSub(googleUserInfo.sub())) {
            throw new AlreadyRegisteredUserException();
        }

        User user = userRepository.save(User.builder()
                .googleSub(googleUserInfo.sub())
                .email(googleUserInfo.email())
                .build());

        return issueTokens(user, true);
    }

    public JwtTokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidRefreshTokenException();
        }

        return issueTokens(user, false);
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);
        user.updateRefreshToken(null);
    }

    private JwtTokenResponse issueTokens(User user, boolean isNewUser) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);
        return JwtTokenResponse.of(accessToken, refreshToken, isNewUser, userStatsReader.toResponse(user));
    }
}
