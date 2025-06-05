package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import java.time.LocalDateTime;

/**
 * HS Code 관련 뉴스 DTO
 */
public record HsCodeNewsDto(
    String title,
    String summary,
    String url,
    String source,
    LocalDateTime publishedAt) {
  // 검증을 위한 compact canonical constructor
  public HsCodeNewsDto {
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("뉴스 제목은 필수입니다");
    }
    if (summary == null || summary.trim().isEmpty()) {
      throw new IllegalArgumentException("뉴스 요약은 필수입니다");
    }
    if (url == null || url.trim().isEmpty()) {
      throw new IllegalArgumentException("뉴스 URL은 필수입니다");
    }
    if (source == null || source.trim().isEmpty()) {
      throw new IllegalArgumentException("뉴스 출처는 필수입니다");
    }
  }
}