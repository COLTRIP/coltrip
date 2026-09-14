package com.coltrip.backend.user.service;

import static org.junit.jupiter.api.Assertions.*;

import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.auth.service.AuthService;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class UserAuthConcurrencyTest {
    @Autowired UserRepository users;
    @Autowired UserService profiles;
    @Autowired AuthService auth;
    @Autowired JwtProvider jwt;
    @Autowired PlatformTransactionManager manager;
    @Autowired DataSource dataSource;

    private TransactionTemplate tx;
    private Long userId;
    private String original;

    @BeforeEach
    void setUp() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("TEST_REQUIRE_MYSQL"))) {
            try (Connection connection = dataSource.getConnection()) {
                assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            }
        }
        tx = new TransactionTemplate(manager);
        String suffix = UUID.randomUUID().toString();
        userId = users.save(User.builder().googleSub(suffix).email(suffix + "@example.test").build()).getId();
        original = jwt.createRefreshToken(userId);
        tx.executeWithoutResult(s -> users.findByIdForUpdate(userId).orElseThrow().updateRefreshToken(original));
    }

    @AfterEach
    void cleanUp() {
        users.findById(userId).ifPresent(users::delete);
    }

    static Stream<Arguments> interleavings() {
        return Stream.of(true, false).flatMap(nickname -> Stream.of(true, false)
                .flatMap(logout -> Stream.of(true, false)
                        .map(authFirst -> Arguments.of(nickname, logout, authFirst))));
    }

    @ParameterizedTest(name = "nickname={0}, logout={1}, authFirst={2}")
    @MethodSource("interleavings")
    void profileAndAuthPreserveBothChanges(boolean nickname, boolean logout, boolean authFirst) throws Exception {
        String[] expected = new String[1];
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?> waiting = tx.execute(s -> {
                if (authFirst) {
                    expected[0] = changeAuth(logout);
                } else {
                    changeProfile(nickname);
                }
                CountDownLatch started = new CountDownLatch(1);
                Future<?> result = pool.submit(() -> {
                    started.countDown();
                    if (authFirst) changeProfile(nickname);
                    else expected[0] = changeAuth(logout);
                });
                await(started);
                // The second request cannot commit until the first user's lock is released.
                assertThrows(TimeoutException.class, () -> result.get(200, TimeUnit.MILLISECONDS));
                return result;
            });
            waiting.get(15, TimeUnit.SECONDS);
        }
        User stored = users.findById(userId).orElseThrow();
        assertEquals(expected[0], stored.getRefreshToken());
        if (nickname) assertEquals("updated", stored.getNickname());
        else assertFalse(stored.isAlternativeNotificationEnabled());
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(original));
        if (!logout) assertNotNull(auth.refresh(expected[0]).accessToken());
    }

    @Test
    void nicknameAndNotificationChangesAreBothPreserved() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?> waiting = tx.execute(s -> {
                profiles.updateNotificationSettings(userId, false);
                CountDownLatch started = new CountDownLatch(1);
                Future<?> result = pool.submit(() -> {
                    started.countDown();
                    profiles.updateNickname(userId, "updated");
                });
                await(started);
                assertThrows(TimeoutException.class, () -> result.get(200, TimeUnit.MILLISECONDS));
                return result;
            });
            waiting.get(15, TimeUnit.SECONDS);
        }
        User stored = users.findById(userId).orElseThrow();
        assertEquals("updated", stored.getNickname());
        assertFalse(stored.isAlternativeNotificationEnabled());
        assertEquals(original, stored.getRefreshToken());
    }

    @Test
    void profileUpdateWaitingOnDeletionCannotRestoreUser() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?> waiting = tx.execute(s -> {
                profiles.deleteMe(userId);
                CountDownLatch started = new CountDownLatch(1);
                Future<?> result = pool.submit(() -> {
                    started.countDown();
                    assertThrows(UnauthorizedException.class, () -> profiles.updateNickname(userId, "updated"));
                });
                await(started);
                assertThrows(TimeoutException.class, () -> result.get(200, TimeUnit.MILLISECONDS));
                return result;
            });
            waiting.get(15, TimeUnit.SECONDS);
        }
        assertFalse(users.existsById(userId));
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(original));
    }

    private void changeProfile(boolean nickname) {
        if (nickname) profiles.updateNickname(userId, "updated");
        else profiles.updateNotificationSettings(userId, false);
    }

    private String changeAuth(boolean logout) {
        if (logout) {
            auth.logout(userId);
            return null;
        }
        return auth.refresh(original).refreshToken();
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
