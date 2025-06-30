package com.hscoderadar.domain.notification.dto;

import com.hscoderadar.domain.user.entity.UserSettings;
import lombok.Builder;

import java.time.LocalTime;

public class NotificationSettingsDto {

  @Builder
  public record Response(
      boolean smsNotificationEnabled,
      boolean emailNotificationEnabled,
      String notificationFrequency,
      LocalTime notificationTime) {
    public static Response from(UserSettings settings) {
      return Response.builder()
          .smsNotificationEnabled(settings.isSmsNotificationEnabled())
          .emailNotificationEnabled(settings.isEmailNotificationEnabled())
          .notificationFrequency(settings.getNotificationFrequency())
          .notificationTime(settings.getNotificationTime())
          .build();
    }
  }

  public record UpdateRequest(
      boolean smsNotificationEnabled,
      boolean emailNotificationEnabled,
      String notificationFrequency,
      LocalTime notificationTime) {
  }
}