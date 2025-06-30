package com.hscoderadar.domain.notification.dto.request;

import java.time.LocalTime;

public record NotificationSettingsUpdateRequest(
    boolean smsNotificationEnabled,
    boolean emailNotificationEnabled,
    String notificationFrequency,
    LocalTime notificationTime) {
}