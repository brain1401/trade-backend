package com.hscoderadar.domain.auth.dto.request;

import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.entity.User.RegistrationType;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;

@Data
public class SignUpRequest {
    
    private String email;
    private String password;
    private String name;

    /**
     * DTO를 User 엔티티로 변환하는 메서드
     * @param passwordEncoder 비밀번호 암호화를 위한 인코더
     * @return User 엔티티
     */
    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(this.email)
                .passwordHash(passwordEncoder.encode(this.password)) // 비밀번호 암호화
                .name(this.name)
                .registrationType(RegistrationType.SELF) // 자체 회원가입 유형으로 설정
                .build();
    }
}