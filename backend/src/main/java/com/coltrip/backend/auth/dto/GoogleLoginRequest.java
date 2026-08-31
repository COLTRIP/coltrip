package com.coltrip.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        @NotNull AuthIntent intent
) {
}
