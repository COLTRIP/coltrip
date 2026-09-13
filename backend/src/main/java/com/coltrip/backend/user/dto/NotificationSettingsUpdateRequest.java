package com.coltrip.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationSettingsUpdateRequest(
        @NotNull Boolean alternativeNotificationEnabled
) {
}
