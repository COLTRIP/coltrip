package com.coltrip.backend.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean
    public RestClient aiRestClient(AiProperties properties) {
        if (!Set.of("mock", "real").contains(properties.dataSource())) {
            throw new IllegalArgumentException("ai.data-source must be mock or real");
        }
        if (properties.connectTimeoutMillis() <= 0 || properties.readTimeoutMillis() <= 0) {
            throw new IllegalArgumentException("AI timeouts must be positive");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMillis());
        factory.setReadTimeout(properties.readTimeoutMillis());
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (StringUtils.hasText(properties.baseUrl())) {
            builder.baseUrl(properties.baseUrl());
        }
        return builder.build();
    }

    @Bean
    public Clock alternativeClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
