package com.coltrip.backend.push.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.push.DeviceToken;
import com.coltrip.backend.domain.push.DeviceTokenRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeviceTokenServiceTest {

    private final DeviceTokenRepository deviceTokens = mock(DeviceTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DeviceTokenService service = new DeviceTokenService(deviceTokens, users);

    @Test
    void registerCreatesNewRowWhenTokenUnknown() {
        User user = User.builder().googleSub("g").email("a@a.com").build();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(deviceTokens.findByToken("tok")).thenReturn(Optional.empty());

        service.register(1L, "tok");

        verify(deviceTokens).save(any(DeviceToken.class));
    }

    @Test
    void registerReassignsOwnerWhenTokenAlreadyExists() {
        User oldOwner = User.builder().googleSub("g1").email("old@a.com").build();
        User newOwner = User.builder().googleSub("g2").email("new@a.com").build();
        DeviceToken existing = DeviceToken.builder().user(oldOwner).token("tok").build();
        when(users.findById(2L)).thenReturn(Optional.of(newOwner));
        when(deviceTokens.findByToken("tok")).thenReturn(Optional.of(existing));

        service.register(2L, "tok");

        assertEquals(newOwner, existing.getUser());
        verify(deviceTokens, never()).save(any());
    }

    @Test
    void registerUnknownUserThrows() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> service.register(99L, "tok"));
    }

    @Test
    void unregisterDelegatesToOwnedDelete() {
        service.unregister(1L, "tok");

        verify(deviceTokens).deleteByUser_IdAndToken(1L, "tok");
    }
}
