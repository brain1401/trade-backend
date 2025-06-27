package com.hscoderadar.domain.auth.dto.response;

import com.hscoderadar.domain.auth.service.AuthService.TokenRefreshResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 응답 DTO")
public record RefreshResponse(
    @Schema(description = "새로 발급된 Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...") String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer") String tokenType,

    @Schema(description = "Access Token 만료 시간(초)", example = "1800") long expiresIn,

    @Schema(description = "'로그인 기억하기' 활성화 여부", example = "true") boolean rememberMe) {

  public static RefreshResponse from(TokenRefreshResult result) {
    return new RefreshResponse(
        result.tokenInfo().accessToken(),
        "Bearer",
        1800L,
        result.rememberMe());
  }
}