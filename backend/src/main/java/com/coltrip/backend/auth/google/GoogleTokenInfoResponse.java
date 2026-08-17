package com.coltrip.backend.auth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleTokenInfoResponse(
        String sub,
        String email,
        String name,
        String picture,
        String aud,
        String iss
) {
}
