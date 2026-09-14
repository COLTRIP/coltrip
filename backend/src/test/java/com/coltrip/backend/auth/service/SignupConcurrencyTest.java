package com.coltrip.backend.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coltrip.backend.auth.controller.AuthController;
import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.service.UserStatsReader;
import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
class SignupConcurrencyTest {
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @MockitoSpyBean UserRepository users;
    @MockitoSpyBean UserStatsReader stats;
    @MockitoBean GoogleTokenVerifier google;
    private MockMvc mvc;
    private String sub;

    @BeforeEach
    void setUp() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("TEST_REQUIRE_MYSQL"))) {
            try (Connection c = dataSource.getConnection()) {
                assertEquals("MySQL", c.getMetaData().getDatabaseProductName());
            }
        }
        sub = UUID.randomUUID().toString();
        when(google.verify("test-token")).thenReturn(new GoogleUserInfo(sub, sub + "@example.test"));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void cleanUp() {
        users.findByGoogleSub(sub).ifPresent(users::delete);
    }

    @Test
    void concurrentSignupReturnsOneSuccessAndOneConflict() throws Exception {
        CountDownLatch checked = new CountDownLatch(2);
        AtomicInteger checks = new AtomicInteger();
        // Execute a real non-locking DB read, then let both inserts race.
        // Later recovery checks must not wait on the initial barrier.
        doAnswer(inv -> {
            boolean exists = jdbc.queryForObject("select count(*) from `user` where google_sub = ?",
                    Long.class, sub) > 0;
            if (checks.incrementAndGet() <= 2) {
                checked.countDown();
                assertTrue(checked.await(10, TimeUnit.SECONDS));
            }
            return exists;
        }).when(users).existsByGoogleSub(sub);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> request = () -> {
                var response = signup().andReturn().getResponse();
                if (response.getStatus() == 409) {
                    assertTrue(response.getContentAsString().contains("AlreadyRegisteredUserException"));
                    assertFalse(response.getContentAsString().contains("accessToken"));
                }
                return response.getStatus();
            };
            var first = pool.submit(request);
            var second = pool.submit(request);
            assertEquals(java.util.List.of(200, 409), java.util.stream.Stream.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)).sorted().toList());
        }
        assertEquals(1L, jdbc.queryForObject("select count(*) from `user` where google_sub = ?", Long.class, sub));
        String token = users.findByGoogleSub(sub).orElseThrow().getRefreshToken();
        assertNotNull(token);
        assertNotNull(auth.refresh(token).accessToken());
    }

    @Test
    void existingAccountReturns409WithoutRotatingToken() throws Exception {
        signup().andExpect(status().isOk()).andExpect(jsonPath("$.isNewUser").value(true));
        String token = users.findByGoogleSub(sub).orElseThrow().getRefreshToken();
        signup().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AlreadyRegisteredUserException"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        assertEquals(token, users.findByGoogleSub(sub).orElseThrow().getRefreshToken());
    }

    @Test
    void failureAfterInsertRollsBackAccountAndTokens() throws Exception {
        doThrow(new IllegalStateException("test response failure")).when(stats).toResponse(any());
        signup().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        assertTrue(users.findByGoogleSub(sub).isEmpty());
    }

    @Test
    void unrelatedNotNullViolationStays500() throws Exception {
        when(google.verify("test-token")).thenReturn(new GoogleUserInfo(sub, null));
        signup().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("InternalServerError"));
        assertTrue(users.findByGoogleSub(sub).isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions signup() throws Exception {
        return mvc.perform(post("/api/auth/google").contentType("application/json")
                .content("{\"idToken\":\"test-token\",\"intent\":\"SIGNUP\"}"));
    }
}
