package com.coltrip.backend.user.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.user.dto.NotificationSettingsResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 대체 장소 알림 설정 조회/변경(#34) 검증
@ExtendWith(MockitoExtension.class)
class UserNotificationSettingsTest {

    private static final Long USER_ID = 1L;

    @Mock
    private com.coltrip.backend.domain.visit.VisitRepository visitRepository;

    @Mock
    private com.coltrip.backend.domain.like.SpotLikeRepository spotLikeRepository;

    @Mock
    private com.coltrip.backend.domain.review.ReviewRepository reviewRepository;

    @Mock
    private com.coltrip.backend.user.service.UserStatsReader userStatsReader;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void defaultsToEnabled() {
        User user = User.builder().googleSub("sub").email("a@coltrip.dev").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        NotificationSettingsResponse response = userService.getNotificationSettings(USER_ID);

        assertTrue(response.alternativeNotificationEnabled());
    }

    @Test
    void updatesAndPersistsDisabledState() {
        User user = User.builder().googleSub("sub").email("a@coltrip.dev").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        NotificationSettingsResponse response = userService.updateNotificationSettings(USER_ID, false);

        assertFalse(response.alternativeNotificationEnabled());
        assertFalse(user.isAlternativeNotificationEnabled());
    }

    @Test
    void unknownUserThrows() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.getNotificationSettings(USER_ID));
    }
}
