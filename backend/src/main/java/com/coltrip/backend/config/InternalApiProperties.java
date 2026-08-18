package com.coltrip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal")
public record InternalApiProperties(
        String apiKey
) {
}
