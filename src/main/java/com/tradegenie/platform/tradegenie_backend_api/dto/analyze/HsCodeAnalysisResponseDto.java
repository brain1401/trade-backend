package com.tradegenie.platform.tradegenie_backend_api.dto.analyze;

import java.util.List;
import java.util.Map;

/**
 * HS Code 분석 응답 DTO
 */
public record HsCodeAnalysisResponseDto(
    String sessionId,
    AnalysisStatus analysisStatus,
    String finalCode,
    Integer confidence,
    String description,
    List<SourceInfoDto> sources,
    String reasoning,
    Map<String, AdditionalQuestionDto> additionalQuestions) {
  // 검증을 위한 compact canonical constructor
  public HsCodeAnalysisResponseDto {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      throw new IllegalArgumentException("세션 ID는 필수입니다");
    }
    if (analysisStatus == null) {
      throw new IllegalArgumentException("분석 상태는 필수입니다");
    }
    if (analysisStatus == AnalysisStatus.COMPLETED) {
      if (finalCode == null || finalCode.trim().isEmpty()) {
        throw new IllegalArgumentException("완료된 분석에는 최종 HS Code가 필요합니다");
      }
      if (confidence == null || confidence < 0 || confidence > 100) {
        throw new IllegalArgumentException("신뢰도는 0-100 사이의 값이어야 합니다");
      }
    }
    if (analysisStatus == AnalysisStatus.NEEDS_MORE_INFO) {
      if (additionalQuestions == null || additionalQuestions.isEmpty()) {
        throw new IllegalArgumentException("추가 정보가 필요한 경우 질문이 제공되어야 합니다");
      }
    }
  }

  public enum AnalysisStatus {
    IN_PROGRESS, // 분석 진행 중
    NEEDS_MORE_INFO, // 추가 정보 필요
    COMPLETED, // 분석 완료
    ERROR // 분석 오류
  }
}
