package com.hscoderadar.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 요약 정보")
public record UserSummary(String name, String email, boolean phoneVerified, boolean rememberMe) {
}