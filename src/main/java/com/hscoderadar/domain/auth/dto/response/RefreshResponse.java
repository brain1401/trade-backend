package com.hscoderadar.domain.auth.dto.response;

import com.hscoderadar.domain.auth.service.AuthService.TokenRefreshResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "토큰 갱신 응답 DTO")
public class RefreshResponse {

  @Schema(description = "새로 발급된 Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...")
  private final String accessToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private final String tokenType = "Bearer";

  @Schema(description = "Access Token 만료 시간(초)", example = "1800")
  private final long expiresIn = 1800;

  @Schema(description = "'로그인 기억하기' 활성화 여부", example = "true")
  private final boolean rememberMe;

  public static RefreshResponse from(TokenRefreshResult result) {
    return RefreshResponse.builder()
        .accessToken(result.tokenInfo().accessToken())
        .rememberMe(result.rememberMe())
        .build();
  }
}