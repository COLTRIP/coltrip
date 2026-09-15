package com.coltrip.backend.push.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.domain.push.DeviceToken;
import com.coltrip.backend.domain.push.DeviceTokenRepository;
import com.coltrip.backend.domain.user.User;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PushSenderTest {

    private final DeviceTokenRepository deviceTokens = mock(DeviceTokenRepository.class);
    private final User user = user(1L);

    @Test
    void unconfiguredFcmSkipsWithoutTouchingRepository() {
        PushSender sender = new PushSender(Optional.empty(), deviceTokens);

        sender.sendAlternativeSuggestion(user, 10L);

        verifyNoInteractions(deviceTokens);
    }

    @Test
    void sendsToEveryRegisteredDeviceOfTheUser() throws Exception {
        FirebaseMessaging client = mock(FirebaseMessaging.class);
        DeviceToken token1 = deviceToken(1L, "tok1");
        DeviceToken token2 = deviceToken(2L, "tok2");
        when(deviceTokens.findByUser_Id(1L)).thenReturn(List.of(token1, token2));
        PushSender sender = new PushSender(Optional.of(client), deviceTokens);

        sender.sendAlternativeSuggestion(user, 10L);

        verify(client, times(2)).send(any());
        verify(deviceTokens, never()).delete(any());
    }

    @Test
    void unregisteredTokenErrorDeletesTheDeviceToken() throws Exception {
        FirebaseMessaging client = mock(FirebaseMessaging.class);
        DeviceToken token = deviceToken(1L, "tok1");
        when(deviceTokens.findByUser_Id(1L)).thenReturn(List.of(token));
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(client.send(any())).thenThrow(error);
        PushSender sender = new PushSender(Optional.of(client), deviceTokens);

        sender.sendAlternativeSuggestion(user, 10L);

        verify(deviceTokens).delete(token);
    }

    @Test
    void otherErrorCodesDoNotDeleteTheToken() throws Exception {
        FirebaseMessaging client = mock(FirebaseMessaging.class);
        DeviceToken token = deviceToken(1L, "tok1");
        when(deviceTokens.findByUser_Id(1L)).thenReturn(List.of(token));
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(client.send(any())).thenThrow(error);
        PushSender sender = new PushSender(Optional.of(client), deviceTokens);

        sender.sendAlternativeSuggestion(user, 10L);

        verify(deviceTokens, never()).delete(any());
    }

    private User user(Long id) {
        User user = User.builder().googleSub("g" + id).email(id + "@a.com").build();
        setId(user, id);
        return user;
    }

    private DeviceToken deviceToken(Long id, String token) {
        DeviceToken deviceToken = DeviceToken.builder().user(user).token(token).build();
        setId(deviceToken, id);
        return deviceToken;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
