package com.hscoderadar.domain.auth.dto.response;

import com.hscoderadar.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {

  @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...")
  private final String accessToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private final String tokenType = "Bearer";

  @Schema(description = "Access Token 만료 시간(초)", example = "1800")
  private final long expiresIn = 1800;

  @Schema(description = "사용자 정보")
  private final UserResponse user;

  public static LoginResponse of(String accessToken, User user) {
    return LoginResponse.builder()
        .accessToken(accessToken)
        .user(UserResponse.from(user))
        .build();
  }
}