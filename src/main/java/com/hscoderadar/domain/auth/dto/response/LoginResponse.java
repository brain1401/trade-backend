package com.hscoderadar.domain.auth.dto.response;

import com.hscoderadar.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...") String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer") String tokenType,

    @Schema(description = "Access Token 만료 시간(초)", example = "1800") long expiresIn,

    @Schema(description = "사용자 정보") UserResponse user) {

  public static LoginResponse of(String accessToken, User user) {
    return new LoginResponse(
        accessToken,
        "Bearer",
        1800L,
        UserResponse.from(user));
  }
}