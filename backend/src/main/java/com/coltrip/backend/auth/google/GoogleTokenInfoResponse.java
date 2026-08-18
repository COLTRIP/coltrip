package com.coltrip.backend.auth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleTokenInfoResponse(
        String sub,
        String email,
        String aud,
        String iss
) {
}
