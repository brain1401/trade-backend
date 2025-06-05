package com.tradegenie.platform.tradegenie_backend_api.dto.analyze;

/**
 * 출처 정보 DTO
 */
public record SourceInfoDto(
    String url,
    String title,
    String siteName) {
  // 검증을 위한 compact canonical constructor
  public SourceInfoDto {
    if (url == null || url.trim().isEmpty()) {
      throw new IllegalArgumentException("출처 URL은 필수입니다");
    }
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("출처 제목은 필수입니다");
    }
  }
}