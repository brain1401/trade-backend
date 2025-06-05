package com.tradegenie.platform.tradegenie_backend_api.dto.auth;

/**
 * 인증 응답 DTO (로그인/회원가입 성공 시)
 */
public record AuthResponseDto(
    Long userId,
    String email,
    String name,
    String companyName,
    String accessToken,
    String refreshToken) {
  // 검증을 위한 compact canonical constructor
  public AuthResponseDto {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 필수입니다");
    }
    if (accessToken == null || accessToken.trim().isEmpty()) {
      throw new IllegalArgumentException("액세스 토큰은 필수입니다");
    }
  }
}