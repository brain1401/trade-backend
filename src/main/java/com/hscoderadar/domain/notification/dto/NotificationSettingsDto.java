package com.hscoderadar.domain.notification.dto;

import com.hscoderadar.domain.user.entity.UserSettings;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

public class NotificationSettingsDto {

    @Getter
    @Builder
    public static class Response {
        private final boolean smsNotificationEnabled;
        private final boolean emailNotificationEnabled;
        private final String notificationFrequency;
        private final LocalTime notificationTime;

        public static Response from(UserSettings settings) {
            return Response.builder()
                    .smsNotificationEnabled(settings.isSmsNotificationEnabled())
                    .emailNotificationEnabled(settings.isEmailNotificationEnabled())
                    .notificationFrequency(settings.getNotificationFrequency())
                    .notificationTime(settings.getNotificationTime())
                    .build();
        }
    }

    @Getter
    public static class UpdateRequest {
        private boolean smsNotificationEnabled;
        private boolean emailNotificationEnabled;
        private String notificationFrequency;
        private LocalTime notificationTime;
    }
}