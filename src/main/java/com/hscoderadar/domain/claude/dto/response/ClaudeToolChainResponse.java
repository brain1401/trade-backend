package com.hscoderadar.domain.claude.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Claude 도구 체이닝 응답 DTO
 * 
 * Claude API의 다중 도구 연계 실행 응답 정보
 */
@Schema(description = "Claude 도구 체이닝 응답")
public record ClaudeToolChainResponse(

    @Schema(description = "응답 ID", example = "chain_123456") String responseId,

    @Schema(description = "최종 답변", example = "서울의 현재 날씨는 맑음이고 기온은 22도입니다. 가벼운 겉옷을 입고 외출하시는 걸 추천드립니다.") String finalAnswer,

    @Schema(description = "체이닝 실행 단계들") List<ChainStep> executionSteps,

    @Schema(description = "사용된 도구 목록") List<String> usedTools,

    @Schema(description = "총 실행 시간 (밀리초)", example = "5000") Long totalExecutionTimeMs,

    @Schema(description = "체이닝 성공 여부", example = "true") Boolean isSuccessful,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "생성 시각") LocalDateTime createdAt,

    @Schema(description = "체이닝 효율성 점수 (0.0-1.0)", example = "0.88") Double efficiencyScore) {

  /**
   * 체이닝 실행 단계 정보
   */
  @Schema(description = "체이닝 실행 단계")
  public record ChainStep(

      @Schema(description = "단계 번호", example = "1") Integer stepNumber,

      @Schema(description = "사용된 도구명", example = "weather_tool") String toolName,

      @Schema(description = "도구 입력 파라미터") Map<String, Object> toolInput,

      @Schema(description = "도구 실행 결과") Object toolOutput,

      @Schema(description = "단계 실행 시간 (밀리초)", example = "1200") Long executionTimeMs,

      @Schema(description = "단계 상태", example = "SUCCESS", allowableValues = {
          "SUCCESS", "FAILED", "SKIPPED" }) String status,

      @Schema(description = "오류 메시지 (실패 시)") String errorMessage,

      @Schema(description = "다음 단계로의 추론", example = "날씨 정보를 바탕으로 의상 추천 도구를 실행합니다.") String reasoningToNext){

    /**
     * 단계 기본값 적용 생성자
     */
    public ChainStep {
      if (stepNumber == null) {
        stepNumber = 0;
      }
      if (status == null) {
        status = "SUCCESS";
      }
      if (executionTimeMs == null) {
        executionTimeMs = 0L;
      }
    }

    /**
     * 단계 성공 여부 확인
     */
    public boolean isSuccessful() {
      return "SUCCESS".equals(status);
    }

    /**
     * 단계 실패 여부 확인
     */
    public boolean isFailed() {
      return "FAILED".equals(status);
    }
  }

  /**
   * 기본값 적용 생성자
   */
  public ClaudeToolChainResponse {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (isSuccessful == null) {
      isSuccessful = true;
    }
    if (efficiencyScore == null) {
      efficiencyScore = 0.0;
    }
  }

  /**
   * 실행 단계 수 확인
   */
  public int getStepCount() {
    return executionSteps != null ? executionSteps.size() : 0;
  }

  /**
   * 실패한 단계 수 확인
   */
  public long getFailedStepCount() {
    return executionSteps != null ? executionSteps.stream().filter(ChainStep::isFailed).count() : 0;
  }
}