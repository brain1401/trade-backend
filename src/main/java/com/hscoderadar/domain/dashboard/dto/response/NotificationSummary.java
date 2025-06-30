package com.hscoderadar.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "알림 통계")
public record NotificationSummary(
    long unreadFeeds,
    long highImportanceFeeds,
    boolean smsEnabled,
    boolean emailEnabled,
    LocalTime notificationTime) {
}