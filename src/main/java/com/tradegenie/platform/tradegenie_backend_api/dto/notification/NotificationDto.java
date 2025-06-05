package com.tradegenie.platform.tradegenie_backend_api.dto.notification;

import java.time.LocalDateTime;

/**
 * 알림 조회 응답 DTO
 */
public record NotificationDto(
    Long id,
    String title,
    String content,
    ChangeType changeType,
    Boolean isRead,
    Boolean isSent,
    LocalDateTime sentAt,
    LocalDateTime createdAt,
    BookmarkSummaryDto bookmark) {
  // 검증을 위한 compact canonical constructor
  public NotificationDto {
    if (id == null) {
      throw new IllegalArgumentException("알림 ID는 필수입니다");
    }
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("알림 제목은 필수입니다");
    }
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("알림 내용은 필수입니다");
    }
    if (changeType == null) {
      throw new IllegalArgumentException("변동 유형은 필수입니다");
    }
  }

  public enum ChangeType {
    REGULATION, TARIFF, NEWS
  }
}
