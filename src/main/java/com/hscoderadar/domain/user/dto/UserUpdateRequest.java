package com.hscoderadar.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UserUpdateRequest {

    @Size(min = 2, max = 10, message = "이름은 2자 이상 10자 이하로 입력해주세요.")
    private String name;

    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$",
             message = "비밀번호는 8~15자리의 영문, 숫자, 특수문자 조합이어야 합니다.")
    private String password;
}
