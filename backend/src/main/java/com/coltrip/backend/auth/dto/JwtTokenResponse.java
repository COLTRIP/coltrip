package com.coltrip.backend.auth.dto;

import com.coltrip.backend.domain.user.User;

public record JwtTokenResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        UserResponse user
) {
    public static JwtTokenResponse of(String accessToken, String refreshToken, boolean isNewUser, User user) {
        return new JwtTokenResponse(accessToken, refreshToken, isNewUser, UserResponse.from(user));
    }
}
