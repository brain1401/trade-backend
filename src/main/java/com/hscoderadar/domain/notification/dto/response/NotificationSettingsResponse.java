package com.hscoderadar.domain.notification.dto.response;

import com.hscoderadar.domain.user.entity.UserSettings;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record NotificationSettingsResponse(
    boolean smsNotificationEnabled,
    boolean emailNotificationEnabled,
    String notificationFrequency,
    LocalTime notificationTime) {
  public static NotificationSettingsResponse from(UserSettings settings) {
    return NotificationSettingsResponse.builder()
        .smsNotificationEnabled(settings.isSmsNotificationEnabled())
        .emailNotificationEnabled(settings.isEmailNotificationEnabled())
        .notificationFrequency(settings.getNotificationFrequency())
        .notificationTime(settings.getNotificationTime())
        .build();
  }
}