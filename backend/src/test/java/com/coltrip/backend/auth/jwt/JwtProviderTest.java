package com.coltrip.backend.auth.jwt;

import static org.junit.jupiter.api.Assertions.*;

import com.coltrip.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtProviderTest {
    private static final String SECRET = "test-only-jwt-secret-for-rotation-12345678901234567890";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void tokensIssuedAtTheSameInstantHaveDistinctIds(boolean refresh) {
        var provider = new JwtProvider(new JwtProperties(SECRET, 3600, 1209600),
                Clock.fixed(Instant.now(), ZoneOffset.UTC));
        String first = refresh ? provider.createRefreshToken(1L) : provider.createAccessToken(1L);
        String second = refresh ? provider.createRefreshToken(1L) : provider.createAccessToken(1L);
        Claims a = claims(first);
        Claims b = claims(second);

        assertEquals(a.getIssuedAt(), b.getIssuedAt());
        assertEquals(a.getExpiration(), b.getExpiration());
        assertNotNull(a.getId());
        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(first, second);
        assertTrue(provider.validateToken(first));
        assertEquals(1L, provider.getUserId(first));
        assertEquals(refresh, provider.isRefreshToken(first));
        assertEquals(!refresh, provider.isAccessToken(first));
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
    }
}
