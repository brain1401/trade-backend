package com.tradegenie.platform.tradegenie_backend_api.dto.bookmark;

import java.util.List;

/**
 * 북마크 생성 요청 DTO
 */
public record CreateBookmarkDto(
    String hsCode,
    String productName,
    List<String> monitoringKeywords) {
  // 검증을 위한 compact canonical constructor
  public CreateBookmarkDto {
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 필수입니다");
    }
    if (productName.length() > 255) {
      throw new IllegalArgumentException("제품명은 255자를 초과할 수 없습니다");
    }
    if (hsCode != null && hsCode.length() > 20) {
      throw new IllegalArgumentException("HS Code는 20자를 초과할 수 없습니다");
    }
    if (monitoringKeywords == null || monitoringKeywords.isEmpty()) {
      throw new IllegalArgumentException("모니터링 키워드는 최소 1개 이상이어야 합니다");
    }
    if (monitoringKeywords.stream().anyMatch(keyword -> keyword == null || keyword.trim().isEmpty())) {
      throw new IllegalArgumentException("모니터링 키워드는 빈 값일 수 없습니다");
    }
  }
}