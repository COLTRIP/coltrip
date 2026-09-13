package com.coltrip.backend.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// CI uses MySQL; each service invocation must have its own transaction for lock tests.
@SpringBootTest
class AuthRotationIntegrationTest {
    @Autowired private AuthService auth;
    @Autowired private JwtProvider jwt;
    @Autowired private UserRepository users;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long userId;
    private String original;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        String suffix = UUID.randomUUID().toString();
        userId = users.save(User.builder().googleSub("rotation-" + suffix)
                .email(suffix + "@example.test").build()).getId();
        original = jwt.createRefreshToken(userId);
        tx.executeWithoutResult(status -> users.findByIdForUpdate(userId).orElseThrow()
                .updateRefreshToken(original));
    }

    @AfterEach
    void tearDown() {
        users.deleteById(userId);
    }

    @Test
    void rotationRejectsUsedTokenWithoutInvalidatingTheNewToken() {
        String next = auth.refresh(original).refreshToken();
        assertNotEquals(original, next);
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(original));
        assertEquals(next, storedToken());
        assertNotEquals(next, auth.refresh(next).refreshToken());
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() {
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(jwt.createAccessToken(userId)));
        assertEquals(original, storedToken());
    }

    @Test
    void concurrentRefreshOnlyOneRequestSucceeds() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            var task = (java.util.concurrent.Callable<String>) () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                try {
                    return auth.refresh(original).refreshToken();
                } catch (InvalidRefreshTokenException expected) {
                    return null;
                }
            };
            Future<String> first = pool.submit(task);
            Future<String> second = pool.submit(task);
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                go.countDown();
            }
            String a = first.get(15, TimeUnit.SECONDS);
            String b = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (a == null ? 0 : 1) + (b == null ? 0 : 1));
            assertEquals(a != null ? a : b, storedToken());
            assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(original));
        }
    }

    @Test
    void logoutBeforeWaitingRefreshPreventsTokenResurrection() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<Boolean> waiting = tx.execute(status -> {
                users.findByIdForUpdate(userId).orElseThrow();
                var started = new CountDownLatch(1);
                Future<Boolean> result = pool.submit(() -> {
                    started.countDown();
                    try {
                        auth.refresh(original);
                        return false;
                    } catch (InvalidRefreshTokenException expected) {
                        return true;
                    }
                });
                await(started);
                auth.logout(userId);
                return result;
            });
            assertTrue(waiting.get(15, TimeUnit.SECONDS));
            assertNull(storedToken());
        }
    }

    @Test
    void logoutAfterRefreshRevokesRotatedToken() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            String[] rotated = new String[1];
            Future<?> waiting = tx.execute(status -> {
                rotated[0] = auth.refresh(original).refreshToken();
                var started = new CountDownLatch(1);
                Future<?> result = pool.submit(() -> {
                    started.countDown();
                    auth.logout(userId);
                });
                await(started);
                return result;
            });
            waiting.get(15, TimeUnit.SECONDS);
            assertNull(storedToken());
            assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(rotated[0]));
        }
    }

    private String storedToken() {
        return users.findById(userId).orElseThrow().getRefreshToken();
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
