package com.coltrip.backend.like.dto;

public record LikeResponse(
        Long spotId,
        boolean liked
) {
}
