package com.hscoderadar.domain.auth.dto.response;

import java.time.LocalDateTime;

import com.hscoderadar.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답 DTO")
public record UserResponse(
    @Schema(description = "사용자 이메일", example = "test@example.com") String email,
    @Schema(description = "사용자 이름", example = "홍길동") String name,
    @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg") String profileImage,
    @Schema(description = "휴대폰 인증 여부", example = "true") boolean phoneVerified,
    @Schema(description = "마지막 로그인 시간", example = "2025-07-05T09:28:00") LocalDateTime lastLoggedInAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getEmail(),
        user.getName(),
        user.getProfileImage(),
        user.getPhoneVerified() != null && user.getPhoneVerified(),
        user.getLastLoggedInAt());
  }
}
