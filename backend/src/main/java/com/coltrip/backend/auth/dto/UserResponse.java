package com.coltrip.backend.auth.dto;

import com.coltrip.backend.domain.user.User;

public record UserResponse(
        Long id,
        String email,
        String nickname
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
