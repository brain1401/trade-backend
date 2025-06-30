package com.hscoderadar.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "처리된 개수 응답 DTO")
public record ProcessedCountResponse(
    @Schema(description = "처리된 항목의 수", example = "10") int processedCount) {
}