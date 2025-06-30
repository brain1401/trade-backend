package com.hscoderadar.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OAuth2 소셜 로그인 요청")
public record OAuth2LoginRequest(
    @Schema(description = "로그인 상태 유지 여부. true로 설정 시 Refresh Token의 유효 기간이 길어짐.", example = "false", defaultValue = "false") boolean rememberMe) {
}