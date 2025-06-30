package com.hscoderadar.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 기록 통계")
public record ChatSummary(long totalSessions, long recentSessions30d, long totalMessages) {
}