package com.coltrip.backend.auth.service;

import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
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

    public JwtTokenResponse googleLogin(String idToken) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(idToken);
        boolean isNewUser = !userRepository.existsByGoogleSub(googleUserInfo.sub());

        User user = userRepository.findByGoogleSub(googleUserInfo.sub())
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleSub(googleUserInfo.sub())
                        .email(googleUserInfo.email())
                        .profileImageUrl(googleUserInfo.profileImageUrl())
                        .build()));

        return issueTokens(user, isNewUser);
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
        return JwtTokenResponse.of(accessToken, refreshToken, isNewUser, user);
    }
}
