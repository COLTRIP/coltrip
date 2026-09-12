package com.coltrip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        @DefaultValue("real") String dataSource,
        @DefaultValue("3000") int connectTimeoutMillis,
        @DefaultValue("30000") int readTimeoutMillis
) {
}
