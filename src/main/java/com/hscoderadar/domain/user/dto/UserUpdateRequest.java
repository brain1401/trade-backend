package com.hscoderadar.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자 프로필 수정을 위한 데이터 전송 객체(DTO)
 */
public record UserUpdateRequest(
        @Size(min = 2, max = 10, message = "이름은 2자 이상 10자 이하로 입력해주세요.") String name,

        String currentPassword,

        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$", message = "비밀번호는 8~15자리의 영문, 숫자, 특수문자 조합이어야 합니다.") String newPassword,

        String newPasswordConfirm) {
}
