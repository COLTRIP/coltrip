package com.coltrip.backend.auth.dto;

import com.coltrip.backend.domain.user.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        long visitCount,
        long likeCount
) {
    public static UserResponse of(User user, long visitCount, long likeCount) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), visitCount, likeCount);
    }
}
