package com.coltrip.backend.demo;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class DemoConfiguration {
    // DataSource/JPA 생성 전 검증해야 잘못 연결한 DB에 ddl-auto가 실행되는 것을 막을 수 있다.
    @Bean
    static BeanFactoryPostProcessor demoStartupGuard(Environment environment) {
        return beanFactory -> validate(environment);
    }

    static void validate(Environment env) {
        var settings = Binder.get(env).bind("demo", DemoProperties.class)
                .orElse(new DemoProperties(false, Set.of()));
        var profiles = Arrays.asList(env.getActiveProfiles());
        if (!settings.enabled() && !profiles.contains("demo")) return;
        if (!settings.enabled() || profiles.size() != 1 || !profiles.contains("demo")
                || !"demo".equals(env.getProperty("app.environment"))) {
            throw new IllegalStateException("시연 모드는 demo 단독 프로필과 app.environment=demo에서만 허용됩니다.");
        }
        if (settings.allowedGoogleSubs().isEmpty()
                || settings.allowedGoogleSubs().stream().anyMatch(s -> !StringUtils.hasText(s) || "*".equals(s))) {
            throw new IllegalStateException("시연 허용 Google sub 목록이 필요합니다. 와일드카드는 허용하지 않습니다.");
        }
        String url = env.getProperty("spring.datasource.url", "");
        if (!url.equals(env.getProperty("demo.datasource-url")) || !isolatedDatabase(url)
                || !env.getProperty("spring.datasource.username", "").equals(env.getProperty("demo.datasource-username"))
                || !StringUtils.hasText(env.getProperty("demo.datasource-username"))
                || !StringUtils.hasText(env.getProperty("demo.datasource-password"))
                || !env.getProperty("spring.datasource.password", "").equals(env.getProperty("demo.datasource-password"))) {
            throw new IllegalStateException("시연 전용 coltrip_demo DB URL/계정/암호 설정이 필요합니다.");
        }
        String secret = env.getProperty("demo.jwt-secret", "");
        if (secret.length() < 32 || !secret.equals(env.getProperty("jwt.secret"))) {
            throw new IllegalStateException("32자 이상의 시연 전용 JWT 키가 필요합니다.");
        }
        if (StringUtils.hasText(env.getProperty("fcm.credentials-path"))) {
            throw new IllegalStateException("시연 환경에서는 FCM 자격증명을 설정할 수 없습니다.");
        }
    }

    private static boolean isolatedDatabase(String url) {
        // 인메모리 DB는 자동 테스트/로컬 시연용. 파일 DB와 다른 MySQL 스키마는 거부한다.
        if (url.matches("jdbc:h2:mem:coltrip_demo(?:;(?:MODE=MySQL|NON_KEYWORDS=USER|DB_CLOSE_DELAY=-1|LOCK_TIMEOUT=[0-9]+))*")) return true;
        try {
            if (!url.startsWith("jdbc:mysql://")) return false;
            URI uri = URI.create(url.substring(5));
            return uri.getHost() != null && uri.getUserInfo() == null && uri.getFragment() == null
                    && "/coltrip_demo".equals(uri.getPath());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
