package com.tradegenie.platform.tradegenie_backend_api.dto.notification;

/**
 * 푸시 알림 구독 요청 DTO
 */
public record PushSubscriptionDto(
    String endpoint,
    String p256dhKey,
    String authKey) {
  // 검증을 위한 compact canonical constructor
  public PushSubscriptionDto {
    if (endpoint == null || endpoint.trim().isEmpty()) {
      throw new IllegalArgumentException("푸시 알림 엔드포인트는 필수입니다");
    }
    if (p256dhKey == null || p256dhKey.trim().isEmpty()) {
      throw new IllegalArgumentException("P256DH 키는 필수입니다");
    }
    if (authKey == null || authKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Auth 키는 필수입니다");
    }
  }
}