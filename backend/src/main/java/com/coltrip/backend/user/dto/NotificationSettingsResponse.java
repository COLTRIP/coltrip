package com.coltrip.backend.user.dto;

import com.coltrip.backend.domain.user.User;

public record NotificationSettingsResponse(
        boolean alternativeNotificationEnabled
) {
    public static NotificationSettingsResponse from(User user) {
        return new NotificationSettingsResponse(user.isAlternativeNotificationEnabled());
    }
}
