package com.tradegenie.platform.tradegenie_backend_api.dto.bookmark;

import java.util.List;

/**
 * 북마크 수정 요청 DTO
 */
public record UpdateBookmarkDto(
    String productName,
    List<String> monitoringKeywords,
    Boolean isActive) {
  // 검증을 위한 compact canonical constructor
  public UpdateBookmarkDto {
    if (productName != null && productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 빈 값일 수 없습니다");
    }
    if (productName != null && productName.length() > 255) {
      throw new IllegalArgumentException("제품명은 255자를 초과할 수 없습니다");
    }
    if (monitoringKeywords != null && monitoringKeywords.isEmpty()) {
      throw new IllegalArgumentException("모니터링 키워드가 제공되는 경우 최소 1개 이상이어야 합니다");
    }
    if (monitoringKeywords != null &&
        monitoringKeywords.stream().anyMatch(keyword -> keyword == null || keyword.trim().isEmpty())) {
      throw new IllegalArgumentException("모니터링 키워드는 빈 값일 수 없습니다");
    }
  }
}