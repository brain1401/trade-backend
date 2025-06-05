package com.tradegenie.platform.tradegenie_backend_api.dto.notification;

/**
 * 북마크 요약 정보 DTO (알림에서 사용)
 */
public record BookmarkSummaryDto(
    Long id,
    String hsCode,
    String productName) {
  // 검증을 위한 compact canonical constructor
  public BookmarkSummaryDto {
    if (id == null) {
      throw new IllegalArgumentException("북마크 ID는 필수입니다");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 필수입니다");
    }
  }
}