package com.tradegenie.platform.tradegenie_backend_api.dto.auth;

/**
 * 로그인 요청 DTO
 */
public record LoginRequestDto(
    String email,
    String password) {
  // 검증을 위한 compact canonical constructor
  public LoginRequestDto {
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수입니다");
    }
  }
}