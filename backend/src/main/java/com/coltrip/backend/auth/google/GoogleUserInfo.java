package com.coltrip.backend.auth.google;

public record GoogleUserInfo(
        String sub,
        String email
) {
}
