package com.coltrip.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.auth.service.AuthService;
import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.config.JwtProperties;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.QuietIndexRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.internal.controller.InternalQuietIndexController;
import com.coltrip.backend.internal.service.InternalQuietIndexService;
import com.coltrip.backend.user.service.UserStatsReader;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthenticationHeaderTest {
    private static final String SECRET = "header-test-only-secret-123456789012345678901234567890";
    private static final String KEY = "internal-test-key";
    private static final String BODY = """
            {"tourApiContentId":"123","quietScore":70,"calculatedAt":"2026-01-01T00:00:00"}
            """;
    private UserRepository users;
    private UserStatsReader stats;
    private TouristSpotRepository spots;
    private QuietIndexRepository history;
    private JwtProvider jwt;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        stats = mock(UserStatsReader.class);
        spots = mock(TouristSpotRepository.class);
        history = mock(QuietIndexRepository.class);
        jwt = new JwtProvider(new JwtProperties(SECRET, 3600, 1209600));
        var auth = new AuthService(users, mock(GoogleTokenVerifier.class), jwt, stats,
                mock(com.coltrip.backend.auth.service.SignupTransaction.class));
        var internal = new InternalQuietIndexService(spots, history, new InternalApiProperties(KEY));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth), new InternalQuietIndexController(internal))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Bearer", "Bearer ", "Bearer    ", "Basic token", "invalid", "Bearer invalid"})
    void missingOrInvalidRefreshHeaderReturns401(String header) throws Exception {
        mvc.perform(withHeader(post("/api/auth/refresh"), "Authorization", header))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("InvalidRefreshTokenException"))
                .andExpect(jsonPath("$.message").isString());
        verifyNoInteractions(users, stats);
    }

    @Test
    void expiredOrAccessTokensCannotRefresh() throws Exception {
        var expired = new JwtProvider(new JwtProperties(SECRET, -60, -60));
        for (String token : new String[]{jwt.createAccessToken(1L), expired.createRefreshToken(1L)}) {
            mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("InvalidRefreshTokenException"));
        }
        verifyNoInteractions(users, stats);
    }

    @Test
    void validRefreshHeaderStillRotatesTokens() throws Exception {
        var user = User.builder().googleSub("header-user").email("user@example.test").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        String token = jwt.createRefreshToken(1L);
        user.updateRefreshToken(token);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(stats.toResponse(user)).thenReturn(UserResponse.of(user, 0, 0, null));
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
        org.junit.jupiter.api.Assertions.assertNotEquals(token, user.getRefreshToken());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "wrong-key", "Bearer internal-test-key"})
    void missingOrInvalidInternalHeaderReturns401(String header) throws Exception {
        mvc.perform(withHeader(post("/api/internal/quiet-index").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY), "X-Internal-Api-Key", header))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("InvalidInternalApiKeyException"))
                .andExpect(jsonPath("$.message").isString());
        verifyNoInteractions(spots, history);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void absentServerKeyCannotAuthenticateAnEmptyRequestKey(String configured) throws Exception {
        var service = new InternalQuietIndexService(spots, history, new InternalApiProperties(configured));
        var isolatedMvc = MockMvcBuilders.standaloneSetup(new InternalQuietIndexController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        isolatedMvc.perform(post("/api/internal/quiet-index").header("X-Internal-Api-Key", "")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("InvalidInternalApiKeyException"));
        verifyNoInteractions(spots, history);
    }

    @Test
    void validInternalKeyStillStoresObservation() throws Exception {
        var spot = TouristSpot.builder().tourApiContentId("123").name("Test").address("Busan")
                .latitude(BigDecimal.valueOf(35)).longitude(BigDecimal.valueOf(129))
                .category(Category.CAFE).build();
        ReflectionTestUtils.setField(spot, "id", 1L);
        when(spots.findByTourApiContentIdForUpdate("123")).thenReturn(Optional.of(spot));
        mvc.perform(post("/api/internal/quiet-index").header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quietScore").value(70));
        verify(history).save(any());
    }

    @Test
    void malformedBodyWithValidKeyRemains400() throws Exception {
        mvc.perform(post("/api/internal/quiet-index").header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("InvalidRequestBodyException"));
        verifyNoInteractions(spots, history);
    }

    private MockHttpServletRequestBuilder withHeader(MockHttpServletRequestBuilder request,
                                                    String name, String value) {
        return value == null ? request : request.header(name, value);
    }
}
