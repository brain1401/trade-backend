package com.tradegenie.platform.tradegenie_backend_api.dto.user;

import java.time.LocalDateTime;

/**
 * 사용자 정보 조회 DTO
 */
public record UserDto(
    Long id,
    String email,
    String name,
    String companyName,
    Boolean pushNotificationEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  // 검증을 위한 compact canonical constructor
  public UserDto {
    if (id == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("이름은 필수입니다");
    }
  }
}