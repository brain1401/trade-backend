package com.tradegenie.platform.tradegenie_backend_api.dto.bookmark;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 북마크 조회 응답 DTO
 */
public record BookmarkDto(
    Long id,
    String hsCode,
    String productName,
    List<String> monitoringKeywords,
    Boolean isActive,
    LocalDateTime lastCheckedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  // 검증을 위한 compact canonical constructor
  public BookmarkDto {
    if (id == null) {
      throw new IllegalArgumentException("북마크 ID는 필수입니다");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 필수입니다");
    }
    if (monitoringKeywords == null || monitoringKeywords.isEmpty()) {
      throw new IllegalArgumentException("모니터링 키워드는 필수입니다");
    }
  }
}