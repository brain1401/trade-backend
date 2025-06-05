package com.tradegenie.platform.tradegenie_backend_api.dto.analyze;

import java.util.Map;

/**
 * 추가 질문 응답 요청 DTO
 */
public record AdditionalQuestionAnswerDto(
    String sessionId,
    Map<String, String> answers) {
  // 검증을 위한 compact canonical constructor
  public AdditionalQuestionAnswerDto {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      throw new IllegalArgumentException("세션 ID는 필수입니다");
    }
    if (answers == null || answers.isEmpty()) {
      throw new IllegalArgumentException("최소 1개 이상의 답변이 필요합니다");
    }
  }
}