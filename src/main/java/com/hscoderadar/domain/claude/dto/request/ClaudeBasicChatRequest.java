package com.hscoderadar.domain.claude.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

/**
 * Claude 기본 채팅 요청 DTO
 * 
 * Claude API와의 기본 텍스트 기반 대화 요청 정보
 */
@Schema(description = "Claude 기본 채팅 요청")
public record ClaudeBasicChatRequest(

    @NotBlank(message = "메시지 입력 필수") @Size(max = 100000, message = "메시지는 100,000자 이하 입력 필요") @Schema(description = "사용자 메시지", example = "안녕하세요, Claude!") String message,

    @Schema(description = "사용자 ID (세션 관리용)", example = "user123") String userId,

    @Schema(description = "모델명", example = "claude-3-5-sonnet-20240620", defaultValue = "claude-3-5-sonnet-20240620") String modelName,

    @DecimalMin(value = "0.0", message = "온도값은 0.0 이상 입력 필요") @DecimalMax(value = "2.0", message = "온도값은 2.0 이하 입력 필요") @Schema(description = "온도값 (0.0-2.0)", example = "0.7", defaultValue = "0.7") Double temperature,

    @Min(value = 1, message = "최대 토큰 수는 1 이상 입력 필요") @Max(value = 200000, message = "최대 토큰 수는 200,000 이하 입력 필요") @Schema(description = "최대 토큰 수", example = "4000", defaultValue = "4000") Integer maxTokens,

    @Schema(description = "시스템 메시지", example = "당신은 도움이 되는 AI 어시스턴트입니다.") String systemMessage) {

  /**
   * 기본값 적용 생성자
   */
  public ClaudeBasicChatRequest {
    if (modelName == null || modelName.isBlank()) {
      modelName = "claude-3-5-sonnet-20240620";
    }
    if (temperature == null) {
      temperature = 0.7;
    }
    if (maxTokens == null) {
      maxTokens = 4000;
    }
  }
}