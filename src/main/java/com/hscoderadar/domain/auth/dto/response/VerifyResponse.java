package com.hscoderadar.domain.auth.dto.response;

import java.time.LocalDateTime;

import com.hscoderadar.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 상태 확인 응답 DTO")
public record VerifyResponse(
    @Schema(description = "사용자 이메일", example = "test@example.com") String email,
    @Schema(description = "사용자 이름", example = "홍길동") String name,
    @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg") String profileImage,
    @Schema(description = "휴대폰 인증 여부", example = "true") boolean phoneVerified,
    @Schema(description = "'로그인 기억하기' 활성화 여부", example = "true") boolean rememberMe,
    @Schema(description = "마지막 로그인", example = "2025-07-05T09:28:00") LocalDateTime lastLoggedInAt,
    @Schema(description = "마지막 정보 수정 시간", example = "2025-07-05 09:03:39.267") LocalDateTime updateAt) {

  public static VerifyResponse from(User user) {
    return new VerifyResponse(
        user.getEmail(),
        user.getName(),
        user.getProfileImage(),
        user.getPhoneVerified() != null && user.getPhoneVerified(),
        user.getRememberMeEnabled() != null && user.getRememberMeEnabled(),
        user.getLastLoggedInAt(),
        user.getUpdatedAt());
  }
}
