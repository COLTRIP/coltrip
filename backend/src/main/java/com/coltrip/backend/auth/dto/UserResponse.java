package com.coltrip.backend.auth.dto;

import com.coltrip.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        long visitCount,
        long likeCount,
        @Schema(description = "현재 진행 중인 방문 ID. 없으면 null",
                types = {"integer", "null"}, format = "int64", example = "10")
        Long currentVisitId
) {
    public static UserResponse of(User user, long visitCount, long likeCount,
                                  Long currentVisitId) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getNickname(),
                visitCount, likeCount, currentVisitId
        );
    }
}