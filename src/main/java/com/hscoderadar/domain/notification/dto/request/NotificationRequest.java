package com.hscoderadar.domain.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 통합 알림 요청 DTO
 * Python AI 서버와 기존 시스템 모두 호환
 */
public record NotificationRequest(
    @NotNull @JsonProperty("user_id") Long userId,

    @JsonProperty("bookmark_id") Long bookmarkId,

    @NotBlank @JsonProperty("title") String title,

    @NotBlank @JsonProperty("content") String content,

    @JsonProperty("email_enabled") boolean emailEnabled,

    @JsonProperty("sms_enabled") boolean smsEnabled,

    // Python AI 서버 호환 필드들
    @JsonProperty("task_id") String taskId,

    @JsonProperty("email") String email,

    @JsonProperty("phone_number") String phoneNumber,

    @JsonProperty("subject") String subject,

    @JsonProperty("message") String message) {

  /**
   * Python AI 서버용 생성자
   */
  public static NotificationRequest forPythonServer(
      String taskId,
      Long userId,
      String email,
      String phoneNumber,
      String subject,
      String message) {
    return new NotificationRequest(
        userId,
        null,
        subject != null ? subject : "무역 규제 업데이트 알림",
        message,
        email != null,
        phoneNumber != null,
        taskId,
        email,
        phoneNumber,
        subject,
        message);
  }

  /**
   * 기존 시스템용 생성자
   */
  public static NotificationRequest forLegacySystem(
      Long userId,
      Long bookmarkId,
      String title,
      String content,
      boolean emailEnabled,
      boolean smsEnabled) {
    return new NotificationRequest(
        userId,
        bookmarkId,
        title,
        content,
        emailEnabled,
        smsEnabled,
        null,
        null,
        null,
        title,
        content);
  }

  /**
   * Python AI 서버 호환성 체크
   */
  public boolean isFromPythonServer() {
    return taskId != null;
  }

  /**
   * 제목 반환 (호환성)
   */
  public String getEffectiveTitle() {
    return title != null ? title : subject;
  }

  /**
   * 내용 반환 (호환성)
   */
  public String getEffectiveContent() {
    return content != null ? content : message;
  }
}