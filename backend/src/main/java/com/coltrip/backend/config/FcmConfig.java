package com.coltrip.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

// fcm.credentials-path가 없으면 FirebaseMessaging 빈을 등록하지 않는다(null 반환 시 Spring이
// NullBean으로 처리해 Optional<FirebaseMessaging> 주입이 empty로 해석됨). PushSender가 그 empty를
// 보고 발송을 건너뛰므로, FCM 자격증명 없이도 나머지 기능은 그대로 배포·구동된다.
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "demo.enabled", havingValue = "false", matchIfMissing = true)
    public FirebaseMessaging firebaseMessaging(FcmProperties properties) {
        if (!StringUtils.hasText(properties.credentialsPath())) {
            log.warn("fcm.credentials-path 미설정 - 백그라운드 푸시 발송이 비활성화됩니다.");
            return null;
        }
        try (FileInputStream credentials = new FileInputStream(properties.credentialsPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            log.warn("FCM 자격증명 파일을 읽을 수 없어 백그라운드 푸시 발송이 비활성화됩니다: {}", e.getMessage());
            return null;
        }
    }
}
