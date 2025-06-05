package com.tradegenie.platform.tradegenie_backend_api.dto.analyze;

import java.util.List;

/**
 * 추가 질문 정보 DTO
 */
public record AdditionalQuestionDto(
    String questionKey,
    String question,
    boolean isRequired,
    List<String> options) {
  // 검증을 위한 compact canonical constructor
  public AdditionalQuestionDto {
    if (questionKey == null || questionKey.trim().isEmpty()) {
      throw new IllegalArgumentException("질문 키는 필수입니다");
    }
    if (question == null || question.trim().isEmpty()) {
      throw new IllegalArgumentException("질문 내용은 필수입니다");
    }
    if (options != null && options.isEmpty()) {
      throw new IllegalArgumentException("옵션이 제공되는 경우 최소 1개 이상이어야 합니다");
    }
  }
}
