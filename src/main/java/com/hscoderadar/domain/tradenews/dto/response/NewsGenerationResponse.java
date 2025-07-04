package com.hscoderadar.domain.tradenews.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Python AI 서버 뉴스 생성 응답 DTO
 */
public record NewsGenerationResponse(
    @NotNull String status,

    @NotNull String message,

    @NotNull @Min(0) @JsonProperty("generated_count") Integer generatedCount) {
}