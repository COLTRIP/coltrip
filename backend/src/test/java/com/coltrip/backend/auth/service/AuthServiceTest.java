package com.coltrip.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coltrip.backend.auth.dto.AuthIntent;
import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.exception.AlreadyRegisteredUserException;
import com.coltrip.backend.auth.exception.UserNotRegisteredException;
import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.service.UserStatsReader;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String ID_TOKEN = "id-token";
    private static final String GOOGLE_SUB = "google-sub";
    private static final String EMAIL = "user@gmail.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserStatsReader userStatsReader;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginIntentIssuesTokensForExistingUser() {
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(GOOGLE_SUB, EMAIL);
        User user = User.builder()
                .googleSub(GOOGLE_SUB)
                .email(EMAIL)
                .build();

        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleUserInfo);
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(user));
        stubTokenIssue();

        JwtTokenResponse response = authService.googleLogin(ID_TOKEN, AuthIntent.LOGIN);

        assertAll(
                () -> assertEquals("access-token", response.accessToken()),
                () -> assertEquals("refresh-token", response.refreshToken()),
                () -> assertFalse(response.isNewUser())
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginIntentRejectsUnregisteredUser() {
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(GOOGLE_SUB, EMAIL);

        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleUserInfo);
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());

        assertThrows(UserNotRegisteredException.class,
                () -> authService.googleLogin(ID_TOKEN, AuthIntent.LOGIN));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupIntentCreatesNewUserAndIssuesTokens() {
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(GOOGLE_SUB, EMAIL);

        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleUserInfo);
        when(userRepository.existsByGoogleSub(GOOGLE_SUB)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTokenIssue();

        JwtTokenResponse response = authService.googleLogin(ID_TOKEN, AuthIntent.SIGNUP);

        assertAll(
                () -> assertEquals("access-token", response.accessToken()),
                () -> assertEquals("refresh-token", response.refreshToken()),
                () -> assertTrue(response.isNewUser())
        );
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signupIntentRejectsAlreadyRegisteredUser() {
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(GOOGLE_SUB, EMAIL);

        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleUserInfo);
        when(userRepository.existsByGoogleSub(GOOGLE_SUB)).thenReturn(true);

        assertThrows(AlreadyRegisteredUserException.class,
                () -> authService.googleLogin(ID_TOKEN, AuthIntent.SIGNUP));
        verify(userRepository, never()).save(any(User.class));
    }

    private void stubTokenIssue() {
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(userStatsReader.toResponse(any(User.class)))
                .thenAnswer(invocation -> UserResponse.of(invocation.getArgument(0), 0, 0, null));
    }
}
