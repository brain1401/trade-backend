package com.hscoderadar.domain.claude.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.util.List;

/**
 * Claude 이미지 분석 요청 DTO
 * 
 * Claude API의 이미지 분석 기능 요청 정보
 */
@Schema(description = "Claude 이미지 분석 요청")
public record ClaudeImageAnalysisRequest(

    @NotBlank(message = "텍스트 메시지 입력 필수") @Size(max = 50000, message = "텍스트 메시지는 50,000자 이하 입력 필요") @Schema(description = "이미지에 대한 질문이나 요청", example = "이 이미지에서 무엇을 볼 수 있는지 설명해주세요.") String textMessage,

    @Schema(description = "이미지 URL 목록", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]") List<String> imageUrls,

    @Schema(description = "Base64 인코딩 이미지 데이터 목록") List<String> imageBase64List,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "모델명", example = "claude-3-5-sonnet-20240620", defaultValue = "claude-3-5-sonnet-20240620") String modelName,

    @DecimalMin(value = "0.0", message = "온도값은 0.0 이상 입력 필요") @DecimalMax(value = "2.0", message = "온도값은 2.0 이하 입력 필요") @Schema(description = "온도값 (0.0-2.0)", example = "0.7", defaultValue = "0.7") Double temperature,

    @Min(value = 1, message = "최대 토큰 수는 1 이상 입력 필요") @Max(value = 200000, message = "최대 토큰 수는 200,000 이하 입력 필요") @Schema(description = "최대 토큰 수", example = "4000", defaultValue = "4000") Integer maxTokens) {

  /**
   * 기본값 적용 생성자
   */
  public ClaudeImageAnalysisRequest {
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

  /**
   * 이미지 데이터 존재 여부 확인
   */
  public boolean hasImageData() {
    return (imageUrls != null && !imageUrls.isEmpty()) ||
        (imageBase64List != null && !imageBase64List.isEmpty());
  }
}