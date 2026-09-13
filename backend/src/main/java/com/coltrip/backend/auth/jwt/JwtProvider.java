package com.coltrip.backend.auth.jwt;

import com.coltrip.backend.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "ACCESS";
    private static final String REFRESH_TYPE = "REFRESH";

    private final SecretKey key;
    private final long accessTokenExpireSeconds;
    private final long refreshTokenExpireSeconds;
    private final Clock clock;

    @Autowired
    public JwtProvider(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    JwtProvider(JwtProperties jwtProperties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireSeconds = jwtProperties.accessTokenExpireSeconds();
        this.refreshTokenExpireSeconds = jwtProperties.refreshTokenExpireSeconds();
        this.clock = clock;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, ACCESS_TYPE, accessTokenExpireSeconds);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, REFRESH_TYPE, refreshTokenExpireSeconds);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TYPE.equals(getTokenType(token));
    }

    public Long getUserId(String token) {
        String subject = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }

    private String getTokenType(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .get(TYPE_CLAIM, String.class);
    }

    private String createToken(Long userId, String type, long expireSeconds) {
        Date now = Date.from(clock.instant());
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
