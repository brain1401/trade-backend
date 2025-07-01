package com.hscoderadar.domain.claude.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Claude 구조화된 응답 DTO
 * 
 * Claude API의 구조화된 데이터 출력 응답 정보
 */
@Schema(description = "Claude 구조화된 응답")
public record ClaudeStructuredResponse(

    @Schema(description = "응답 ID", example = "struct_123456") String responseId,

    @Schema(description = "구조화된 데이터") Object structuredData,

    @Schema(description = "원본 텍스트 응답", example = "추출된 정보: 이름은 김철수, 회사는 삼성전자입니다.") String rawResponse,

    @Schema(description = "출력 타입", example = "JSON") String outputType,

    @Schema(description = "검증 결과") ValidationResult validation,

    @Schema(description = "사용된 모델명", example = "claude-3-5-sonnet-20240620") String modelName,

    @Schema(description = "처리 시간 (밀리초)", example = "2000") Long processingTimeMs,

    @Schema(description = "생성 시각") LocalDateTime createdAt,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "신뢰도 점수 (0.0-1.0)", example = "0.92") Double confidenceScore) {

  /**
   * 검증 결과 정보
   */
  @Schema(description = "검증 결과")
  public record ValidationResult(

      @Schema(description = "검증 통과 여부", example = "true") Boolean isValid,

      @Schema(description = "검증 점수 (0.0-1.0)", example = "0.95") Double validationScore,

      @Schema(description = "검증 오류 목록") Map<String, String> validationErrors,

      @Schema(description = "구조 준수 여부", example = "true") Boolean schemaCompliant) {

    /**
     * 검증 결과 기본값 적용 생성자
     */
    public ValidationResult {
      if (isValid == null) {
        isValid = true;
      }
      if (validationScore == null) {
        validationScore = 1.0;
      }
      if (schemaCompliant == null) {
        schemaCompliant = true;
      }
    }

    /**
     * 오류 존재 여부 확인
     */
    public boolean hasErrors() {
      return validationErrors != null && !validationErrors.isEmpty();
    }
  }

  /**
   * 기본값 적용 생성자
   */
  public ClaudeStructuredResponse {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (confidenceScore == null) {
      confidenceScore = 0.0;
    }
  }
}