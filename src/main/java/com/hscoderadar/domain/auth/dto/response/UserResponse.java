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
@Schema(description = "사용자 정보 응답 DTO")
public class UserResponse {

  @Schema(description = "사용자 이메일", example = "test@example.com")
  private final String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private final String name;

  @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
  private final String profileImage;

  @Schema(description = "휴대폰 인증 여부", example = "true")
  private final boolean phoneVerified;

  public static UserResponse from(User user) {
    return UserResponse.builder()
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .phoneVerified(user.getPhoneVerified() != null && user.getPhoneVerified())
        .build();
  }
}