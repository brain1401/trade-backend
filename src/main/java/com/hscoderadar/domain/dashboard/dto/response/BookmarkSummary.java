package com.hscoderadar.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크 통계")
public record BookmarkSummary(long total, long activeMonitoring, long sseGenerated) {
}