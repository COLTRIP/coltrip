package com.coltrip.backend.auth.dto;

public record JwtTokenResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        UserResponse user
) {
    public static JwtTokenResponse of(String accessToken, String refreshToken, boolean isNewUser, UserResponse user) {
        return new JwtTokenResponse(accessToken, refreshToken, isNewUser, user);
    }
}
