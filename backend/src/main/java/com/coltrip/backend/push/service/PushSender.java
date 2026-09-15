package com.coltrip.backend.push.service;

import com.coltrip.backend.domain.push.DeviceToken;
import com.coltrip.backend.domain.push.DeviceTokenRepository;
import com.coltrip.backend.domain.user.User;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// FCM 발송 실패는 여기서 끝나고 방문 완료/취소 등 핵심 흐름으로 전파되지 않는다.
// 재시도 큐는 두지 않는다 - 다음 백그라운드 평가 주기에 여전히 트리거 상태면 자연히 다시 시도된다.
@Component
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final Optional<FirebaseMessaging> messaging;
    private final DeviceTokenRepository deviceTokens;

    public PushSender(Optional<FirebaseMessaging> messaging, DeviceTokenRepository deviceTokens) {
        this.messaging = messaging;
        this.deviceTokens = deviceTokens;
    }

    public void sendAlternativeSuggestion(User user, Long visitId) {
        if (messaging.isEmpty()) {
            log.info("FCM 미설정으로 푸시 발송을 건너뜁니다. visitId={}", visitId);
            return;
        }
        List<DeviceToken> tokens = deviceTokens.findByUser_Id(user.getId());
        for (DeviceToken deviceToken : tokens) {
            send(messaging.get(), deviceToken, visitId);
        }
    }

    private void send(FirebaseMessaging client, DeviceToken deviceToken, Long visitId) {
        Message message = Message.builder()
                .setToken(deviceToken.getToken())
                .setNotification(Notification.builder()
                        .setTitle("더 한적한 곳이 있어요")
                        .setBody("지금 계신 곳이 붐비기 시작했어요. 대체 장소를 확인해보세요.")
                        .build())
                .putData("type", "ALTERNATIVE_SUGGESTION")
                .putData("visitId", visitId.toString())
                .build();
        try {
            client.send(message);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                deviceTokens.delete(deviceToken);
                log.info("무효한 기기 토큰을 삭제했습니다. deviceTokenId={}", deviceToken.getId());
            } else {
                log.warn("푸시 발송 실패. deviceTokenId={}, errorCode={}",
                        deviceToken.getId(), e.getMessagingErrorCode(), e);
            }
        }
    }
}
