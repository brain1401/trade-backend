package com.hscoderadar.domain.claude.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Claude 채팅 응답 DTO
 * 
 * Claude API의 기본 채팅 응답 결과 정보
 */
@Schema(description = "Claude 채팅 응답")
public record ClaudeChatResponse(

    @Schema(description = "응답 ID", example = "resp_123456") String responseId,

    @Schema(description = "Claude의 답변", example = "안녕하세요! 무엇을 도와드릴까요?") String content,

    @Schema(description = "사용된 모델명", example = "claude-3-5-sonnet-20240620") String modelName,

    @Schema(description = "처리 시간 (밀리초)", example = "1500") Long processingTimeMs,

    @Schema(description = "사용된 토큰 수") TokenUsage tokenUsage,

    @Schema(description = "응답 생성 시각") LocalDateTime createdAt,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "추가 메타데이터") Map<String, Object> metadata,

    @Schema(description = "응답 품질 점수 (0.0-1.0)", example = "0.95") Double qualityScore) {

  /**
   * 토큰 사용량 정보
   */
  @Schema(description = "토큰 사용량 정보")
  public record TokenUsage(

      @Schema(description = "입력 토큰 수", example = "25") Integer inputTokens,

      @Schema(description = "출력 토큰 수", example = "150") Integer outputTokens,

      @Schema(description = "총 토큰 수", example = "175") Integer totalTokens,

      @Schema(description = "추정 비용 (USD)", example = "0.0025") Double estimatedCost) {

    /**
     * 토큰 사용량 검증 생성자
     */
    public TokenUsage {
      if (inputTokens == null)
        inputTokens = 0;
      if (outputTokens == null)
        outputTokens = 0;
      if (totalTokens == null)
        totalTokens = inputTokens + outputTokens;
      if (estimatedCost == null)
        estimatedCost = 0.0;
    }
  }
}