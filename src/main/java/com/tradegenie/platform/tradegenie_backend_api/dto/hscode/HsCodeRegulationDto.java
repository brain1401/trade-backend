package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import java.time.LocalDateTime;

/**
 * HS Code 관련 규제 정보 DTO
 */
public record HsCodeRegulationDto(
    String title,
    String summary,
    RegulationType regulationType,
    String url,
    String source,
    LocalDateTime effectiveDate,
    LocalDateTime publishedAt) {
  // 검증을 위한 compact canonical constructor
  public HsCodeRegulationDto {
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("규제 제목은 필수입니다");
    }
    if (summary == null || summary.trim().isEmpty()) {
      throw new IllegalArgumentException("규제 요약은 필수입니다");
    }
    if (regulationType == null) {
      throw new IllegalArgumentException("규제 유형은 필수입니다");
    }
    if (source == null || source.trim().isEmpty()) {
      throw new IllegalArgumentException("규제 출처는 필수입니다");
    }
  }

  public enum RegulationType {
    TARIFF_CHANGE, // 관세 변경
    IMPORT_RESTRICTION, // 수입 제한
    EXPORT_RESTRICTION, // 수출 제한
    SAFETY_REGULATION, // 안전 규제
    ENVIRONMENTAL, // 환경 규제
    LABELING, // 라벨링 규제
    OTHER // 기타
  }
}