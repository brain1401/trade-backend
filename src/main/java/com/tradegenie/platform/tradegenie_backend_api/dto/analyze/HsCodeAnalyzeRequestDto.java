package com.tradegenie.platform.tradegenie_backend_api.dto.analyze;

/**
 * HS Code 분석 요청 DTO
 */
public record HsCodeAnalyzeRequestDto(
    String productDescription,
    String sessionId) {
  // 검증을 위한 compact canonical constructor
  public HsCodeAnalyzeRequestDto {
    if (productDescription == null || productDescription.trim().isEmpty()) {
      throw new IllegalArgumentException("제품 설명은 필수입니다");
    }
    if (productDescription.length() > 1000) {
      throw new IllegalArgumentException("제품 설명은 1000자를 초과할 수 없습니다");
    }
    if (sessionId != null && sessionId.trim().isEmpty()) {
      throw new IllegalArgumentException("세션 ID는 빈 값일 수 없습니다");
    }
  }
}