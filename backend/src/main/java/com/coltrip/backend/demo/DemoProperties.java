package com.coltrip.backend.demo;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("demo")
public record DemoProperties(boolean enabled, Set<String> allowedGoogleSubs) {
    public DemoProperties {
        allowedGoogleSubs = allowedGoogleSubs == null ? Set.of() : Set.copyOf(allowedGoogleSubs);
    }
}
