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
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtProvider jwtProvider;
    private final UserStatsReader userStatsReader;
    private final SignupTransaction signupTransaction;

    public JwtTokenResponse googleLogin(String idToken, AuthIntent intent) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(idToken);

        return switch (intent) {
            case LOGIN -> login(googleUserInfo);
            case SIGNUP -> signup(googleUserInfo);
        };
    }

    private JwtTokenResponse login(GoogleUserInfo googleUserInfo) {
        User user = userRepository.findByGoogleSubForUpdate(googleUserInfo.sub())
                .orElseThrow(UserNotRegisteredException::new);
        return issueTokens(user, false);
    }

    private JwtTokenResponse signup(GoogleUserInfo googleUserInfo) {
        try {
            return signupTransaction.register(googleUserInfo);
        } catch (DataIntegrityViolationException exception) {
            // 실패한 가입 트랜잭션의 롤백 후 새 스냅샷으로 확인한다.
            if (isDuplicateKey(exception) && signupTransaction.isRegistered(googleUserInfo.sub())) {
                throw new AlreadyRegisteredUserException();
            }
            throw exception;
        }
    }

    private boolean isDuplicateKey(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql
                    && ((sql.getErrorCode() == 1062 && "23000".equals(sql.getSQLState()))
                        || "23505".equals(sql.getSQLState()))) {
                return true;
            }
        }
        return false;
    }

    public JwtTokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        // 잠금을 기다리는 동안 토큰이 만료되거나 다른 요청에서 교체될 수 있다.
        if (!jwtProvider.validateToken(refreshToken) || !refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidRefreshTokenException();
        }

        return issueTokens(user, false);
    }

    public void logout(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
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
