package com.hscoderadar.domain.auth.dto.request;

import com.hscoderadar.domain.users.entity.User;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 사용자 회원가입 요청 데이터를 담는 DTO 클래스 (v4.2)
 * 
 * <p>
 * 이 클래스는 자체 회원가입 시 클라이언트로부터 전송되는 사용자 정보를 담으며,
 * 이메일, 비밀번호, 이름을 포함합니다. 비밀번호는 BCrypt로 암호화되어 저장됩니다.
 * 
 * <h3>v4.2 변경사항:</h3>
 * <ul>
 * <li>RegistrationType 제거 - SNS 계정 연동은 별도 테이블로 관리</li>
 * <li>단순화된 사용자 생성 로직</li>
 * </ul>
 * 
 * <h3>요청 형식:</h3>
 * 
 * <pre>{@code
 * {
 *   "email": "newuser@example.com",
 *   "password": "securePassword123!",
 *   "name": "홍길동"
 * }
 * }</pre>
 * 
 * @author HsCodeRadar Team
 * @since 4.2.0
 * @see User
 */
@Data
public class SignUpRequest {

    /**
     * 회원가입할 사용자의 이메일 주소
     * 
     * <p>
     * 이메일은 사용자의 고유 식별자로 사용되며, 시스템 내에서 중복될 수 없습니다.
     * 로그인 시에도 이 이메일을 사용하게 됩니다.
     * 
     * <h3>제약사항:</h3>
     * <ul>
     * <li>null이거나 빈 문자열이면 안됨</li>
     * <li>유효한 이메일 형식이어야 함</li>
     * <li>시스템 내에서 유일해야 함 (중복 불가)</li>
     * <li>최대 255자까지 가능</li>
     * </ul>
     * 
     * @example newuser@example.com
     */
    private String email;

    /**
     * 사용자의 로그인 비밀번호
     * 
     * <p>
     * 클라이언트에서 평문으로 전송되지만, 서버에서 BCrypt 알고리즘을 사용하여
     * 안전하게 해시화되어 데이터베이스에 저장됩니다.
     * 
     * <h3>보안 권장사항:</h3>
     * <ul>
     * <li>최소 8자 이상, 최대 128자 이하</li>
     * <li>영문 대소문자, 숫자, 특수문자 포함</li>
     * <li>일반적인 사전 단어나 개인정보 사용 금지</li>
     * </ul>
     * 
     * @example securePassword123!
     */
    private String password;

    /**
     * 사용자의 실명 또는 표시 이름
     * 
     * <p>
     * 시스템 내에서 사용자를 식별하고 표시하는 데 사용되는 이름입니다.
     * 
     * <h3>제약사항:</h3>
     * <ul>
     * <li>null이거나 빈 문자열이면 안됨</li>
     * <li>최소 1자 이상, 최대 100자 이하</li>
     * </ul>
     * 
     * @example 홍길동
     */
    private String name;

    /**
     * DTO를 User 엔티티로 변환하는 메서드 (v4.2)
     * 
     * <p>
     * 이 메서드는 회원가입 요청 데이터를 실제 데이터베이스에 저장할 수 있는
     * User 엔티티 객체로 변환합니다.
     * 
     * <h3>v4.2 변환 과정:</h3>
     * <ol>
     * <li>이메일과 이름은 그대로 복사</li>
     * <li>비밀번호는 BCrypt로 안전하게 암호화</li>
     * <li>생성 시간과 수정 시간은 JPA에서 자동 설정</li>
     * </ol>
     * 
     * @param passwordEncoder 비밀번호 암호화를 위한 BCrypt 인코더
     * @return 데이터베이스 저장 준비가 완료된 User 엔티티 객체
     * @throws IllegalArgumentException passwordEncoder가 null인 경우
     * 
     * @see User.UserBuilder
     * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
     */
    public User toEntity(PasswordEncoder passwordEncoder) {
        if (passwordEncoder == null) {
            throw new IllegalArgumentException("PasswordEncoder는 null이 될 수 없습니다.");
        }

        return User.builder()
                .email(this.email)
                .passwordHash(passwordEncoder.encode(this.password)) // 비밀번호 암호화
                .name(this.name)
                .build();
    }
}