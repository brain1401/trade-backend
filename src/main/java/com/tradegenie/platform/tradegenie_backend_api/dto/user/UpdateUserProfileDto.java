package com.tradegenie.platform.tradegenie_backend_api.dto.user;

/**
 * 사용자 프로필 수정 요청 DTO
 */
public record UpdateUserProfileDto(
    String name,
    String companyName,
    Boolean pushNotificationEnabled) {
  // 검증을 위한 compact canonical constructor
  public UpdateUserProfileDto {
    if (name != null && name.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 빈 값일 수 없습니다");
    }
    if (name != null && name.length() > 100) {
      throw new IllegalArgumentException("이름은 100자를 초과할 수 없습니다");
    }
    if (companyName != null && companyName.length() > 255) {
      throw new IllegalArgumentException("회사명은 255자를 초과할 수 없습니다");
    }
  }
}