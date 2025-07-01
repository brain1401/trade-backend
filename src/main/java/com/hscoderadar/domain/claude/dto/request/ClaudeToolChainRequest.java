package com.hscoderadar.domain.claude.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.util.List;
import java.util.Map;

/**
 * Claude 도구 체이닝 요청 DTO
 * 
 * Claude API의 다중 도구 연계 실행 요청 정보
 */
@Schema(description = "Claude 도구 체이닝 요청")
public record ClaudeToolChainRequest(

    @NotBlank(message = "메시지 입력 필수") @Size(max = 100000, message = "메시지는 100,000자 이하 입력 필요") @Schema(description = "도구 사용을 요청하는 메시지", example = "오늘 서울의 날씨를 확인하고, 그 결과를 바탕으로 외출 복장을 추천해주세요.") String message,

    @Schema(description = "사용할 도구 목록", example = "[\"weather_tool\", \"clothing_recommendation_tool\", \"calculator_tool\"]") List<String> availableTools,

    @Schema(description = "도구별 설정 파라미터") Map<String, Object> toolConfigurations,

    @Schema(description = "자동 체이닝 활성화 여부", example = "true", defaultValue = "true") Boolean enableAutoChaining,

    @Min(value = 1, message = "최대 체이닝 단계 수는 1 이상 입력 필요") @Max(value = 20, message = "최대 체이닝 단계 수는 20 이하 입력 필요") @Schema(description = "최대 체이닝 단계 수", example = "5", defaultValue = "5") Integer maxChainSteps,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "모델명", example = "claude-3-5-sonnet-20240620", defaultValue = "claude-3-5-sonnet-20240620") String modelName,

    @DecimalMin(value = "0.0", message = "온도값은 0.0 이상 입력 필요") @DecimalMax(value = "2.0", message = "온도값은 2.0 이하 입력 필요") @Schema(description = "온도값 (0.0-2.0)", example = "0.7", defaultValue = "0.7") Double temperature,

    @Min(value = 1, message = "최대 토큰 수는 1 이상 입력 필요") @Max(value = 200000, message = "최대 토큰 수는 200,000 이하 입력 필요") @Schema(description = "최대 토큰 수", example = "4000", defaultValue = "4000") Integer maxTokens) {

  /**
   * 기본값 적용 생성자
   */
  public ClaudeToolChainRequest {
    if (modelName == null || modelName.isBlank()) {
      modelName = "claude-3-5-sonnet-20240620";
    }
    if (temperature == null) {
      temperature = 0.7;
    }
    if (maxTokens == null) {
      maxTokens = 4000;
    }
    if (enableAutoChaining == null) {
      enableAutoChaining = true;
    }
    if (maxChainSteps == null) {
      maxChainSteps = 5;
    }
  }

  /**
   * 도구 설정 존재 여부 확인
   */
  public boolean hasToolConfigurations() {
    return toolConfigurations != null && !toolConfigurations.isEmpty();
  }
}