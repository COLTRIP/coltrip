package com.coltrip.backend.demo;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class DemoConfigurationTest {
    private static final String KEY = "demo-only-test-secret-12345678901234567890";

    private MockEnvironment valid() {
        var env = new MockEnvironment().withProperty("demo.enabled", "true")
                .withProperty("app.environment", "demo").withProperty("demo.allowed-google-subs", "allowed")
                .withProperty("demo.datasource-url", "jdbc:mysql://localhost:3306/coltrip_demo")
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/coltrip_demo")
                .withProperty("demo.datasource-username", "demo_user").withProperty("spring.datasource.username", "demo_user")
                .withProperty("demo.datasource-password", "test-only").withProperty("spring.datasource.password", "test-only")
                .withProperty("demo.jwt-secret", KEY).withProperty("jwt.secret", KEY);
        env.setActiveProfiles("demo");
        return env;
    }

    @Test void defaultOffDoesNotRequireDemoSettings() {
        assertDoesNotThrow(() -> DemoConfiguration.validate(new MockEnvironment()));
    }

    @Test void dedicatedConfigurationIsAccepted() {
        assertDoesNotThrow(() -> DemoConfiguration.validate(valid()));
    }

    @Test void demoYamlResolvesDedicatedEnvironmentVariables() throws IOException {
        var env = new MockEnvironment();
        env.setActiveProfiles("demo");
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("demo-test-env", Map.of(
                "APP_ENVIRONMENT", "demo", "DEMO_ALLOWED_GOOGLE_SUBS", "allowed",
                "DEMO_DATASOURCE_URL", "jdbc:mysql://localhost:3307/coltrip_demo",
                "DEMO_DATASOURCE_USERNAME", "demo_user", "DEMO_DATASOURCE_PASSWORD", "test-only",
                "DEMO_JWT_SECRET", KEY)));
        for (var source : new YamlPropertySourceLoader().load("demo-yaml", new ClassPathResource("application-demo.yml"))) {
            env.getPropertySources().addLast(source);
        }
        assertDoesNotThrow(() -> DemoConfiguration.validate(env));
        assertEquals("true", env.getProperty("demo.enabled"));
        assertEquals("jdbc:mysql://localhost:3307/coltrip_demo", env.getProperty("spring.datasource.url"));
        assertEquals(KEY, env.getProperty("jwt.secret"));
        // application.yml/application-secret.yml에 기본값이 없어 여기서 누락되면 0(즉시 만료)으로 바인딩된다.
        assertTrue(Long.parseLong(env.getProperty("jwt.access-token-expire-seconds")) > 0);
        assertTrue(Long.parseLong(env.getProperty("jwt.refresh-token-expire-seconds")) > 0);
        assertEquals("validate", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("", env.getProperty("fcm.credentials-path"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "production", "local", "test"})
    void enabledOutsideDemoOrMixedProfilesFails(String profile) {
        var env = valid();
        env.setActiveProfiles(profile);
        assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(env));
        env.setActiveProfiles("demo", profile);
        assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(env));
    }

    @Test void cannotDisableRestrictionWhileDemoProfileActive() {
        assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(valid().withProperty("demo.enabled", "false")));
        assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(valid().withProperty("app.environment", "production")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "*", " "})
    void emptyOrWildcardAllowlistFails(String value) {
        assertThrows(RuntimeException.class, () -> DemoConfiguration.validate(valid().withProperty("demo.allowed-google-subs", value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://localhost/coltrip", "jdbc:mysql://localhost/coltrip_demo_other",
            "jdbc:h2:file:./coltrip_demo", "jdbc:h2:mem:other", "jdbc:mysql://user:password@localhost/coltrip_demo"})
    void wrongDatabaseFailsEvenWhenBothUrlsMatch(String url) {
        assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(valid()
                .withProperty("spring.datasource.url", url).withProperty("demo.datasource-url", url)));
    }

    @Test void credentialOverridesAndFcmFail() {
        for (String[] override : new String[][]{{"jwt.secret", "different"}, {"demo.jwt-secret", "short"},
                {"spring.datasource.username", "root"}, {"spring.datasource.password", "other"},
                {"fcm.credentials-path", "do-not-read.json"}}) {
            assertThrows(IllegalStateException.class, () -> DemoConfiguration.validate(valid().withProperty(override[0], override[1])));
        }
    }

    @Test void validationRunsBeforeDatabaseOrOtherSingletonInitialization() {
        var initialized = new AtomicBoolean();
        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(valid().withProperty("app.environment", "production"));
            context.register(DemoConfiguration.class);
            context.registerBean("pretendDataSource", Object.class, () -> { initialized.set(true); return new Object(); });
            assertThrows(Exception.class, context::refresh);
            assertFalse(initialized.get());
        }
    }
}
