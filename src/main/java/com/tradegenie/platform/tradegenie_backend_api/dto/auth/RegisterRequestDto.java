package com.tradegenie.platform.tradegenie_backend_api.dto.auth;

/**
 * 회원가입 요청 DTO
 */
public record RegisterRequestDto(
    String email,
    String password,
    String name,
    String companyName) {
  // 검증을 위한 compact canonical constructor
  public RegisterRequestDto {
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
      throw new IllegalArgumentException("유효한 이메일 형식이어야 합니다");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수입니다");
    }
    if (password.length() < 8) {
      throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다");
    }
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 필수입니다");
    }
    if (name.length() > 100) {
      throw new IllegalArgumentException("이름은 100자를 초과할 수 없습니다");
    }
    if (companyName != null && companyName.length() > 255) {
      throw new IllegalArgumentException("회사명은 255자를 초과할 수 없습니다");
    }
  }
}