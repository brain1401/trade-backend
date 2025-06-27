package com.hscoderadar.domain.auth.dto.response;

import com.hscoderadar.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public record RegisterResponse(
    @Schema(description = "사용자 이메일", example = "test@example.com") String email,

    @Schema(description = "사용자 이름", example = "홍길동") String name,

    @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg") String profileImage) {

  public static RegisterResponse from(User user) {
    return new RegisterResponse(
        user.getEmail(),
        user.getName(),
        user.getProfileImage());
  }
}