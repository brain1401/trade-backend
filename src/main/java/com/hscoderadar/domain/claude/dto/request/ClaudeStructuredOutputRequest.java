package com.hscoderadar.domain.claude.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

/**
 * Claude 구조화된 출력 요청 DTO
 * 
 * Claude API의 구조화된 데이터 출력 요청 정보
 */
@Schema(description = "Claude 구조화된 출력 요청")
public record ClaudeStructuredOutputRequest(

    @NotBlank(message = "메시지 입력 필수") @Size(max = 100000, message = "메시지는 100,000자 이하 입력 필요") @Schema(description = "구조화된 응답을 요청하는 메시지", example = "다음 텍스트에서 사람 이름과 회사명을 추출해주세요: 김철수는 삼성전자에서 일합니다.") String message,

    @NotBlank(message = "출력 형태 입력 필수") @Schema(description = "요청하는 출력 형태", example = "JSON", allowableValues = {
        "JSON", "PERSON_INFO", "COMPANY_INFO", "ANALYSIS_RESULT" }) String outputType,

    @Schema(description = "JSON 스키마 정의 (JSON 타입인 경우)", example = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" }, \"company\": { \"type\": \"string\" } } }") String jsonSchema,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "모델명", example = "claude-3-5-sonnet-20240620", defaultValue = "claude-3-5-sonnet-20240620") String modelName,

    @DecimalMin(value = "0.0", message = "온도값은 0.0 이상 입력 필요") @DecimalMax(value = "2.0", message = "온도값은 2.0 이하 입력 필요") @Schema(description = "온도값 (0.0-2.0)", example = "0.3", defaultValue = "0.3") Double temperature,

    @Min(value = 1, message = "최대 토큰 수는 1 이상 입력 필요") @Max(value = 200000, message = "최대 토큰 수는 200,000 이하 입력 필요") @Schema(description = "최대 토큰 수", example = "4000", defaultValue = "4000") Integer maxTokens){

  /**
   * 기본값 적용 생성자
   */
  public ClaudeStructuredOutputRequest {
    if (modelName == null || modelName.isBlank()) {
      modelName = "claude-3-5-sonnet-20240620";
    }
    if (temperature == null) {
      temperature = 0.3; // 구조화된 출력은 낮은 온도 사용
    }
    if (maxTokens == null) {
      maxTokens = 4000;
    }
  }

  /**
   * JSON 스키마 필요 여부 확인
   */
  public boolean requiresJsonSchema() {
    return "JSON".equalsIgnoreCase(outputType) && (jsonSchema == null || jsonSchema.isBlank());
  }
}