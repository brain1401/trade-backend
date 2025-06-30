package com.hscoderadar.domain.auth.dto.request;

import com.hscoderadar.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AI 기반 무역 규제 레이더 플랫폼 v6.1 회원가입 요청 DTO
 *
 * <p>
 * 📊 v6.1 보안 정책에 따른 회원가입 요청 정보: - 최소 필수 정보만 수집 (이메일, 비밀번호, 이름) - 강화된 비밀번호 정책 적용
 * - 응답에는 최소 사용자
 * 정보만 제공
 *
 * <p>
 * 🛡️ v6.1 보안 특징: - BCrypt 암호화 적용 - 이메일 중복 검증 - 사용자 열거 공격 방지
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 * @see User
 */
public record SignUpRequest(
    /**
     * 회원가입할 사용자의 이메일 주소
     *
     * <p>
     * 이메일은 사용자의 고유 식별자로 사용되며, 시스템 내에서 중복될 수 없음. 로그인 시에도 이 이메일을 사용하게 됨.
     *
     * <h3>제약사항:</h3>
     *
     * <ul>
     * <li>null이거나 빈 문자열이면 안됨
     * <li>유효한 이메일 형식이어야 함
     * <li>시스템 내에서 유일해야 함 (중복 불가)
     * <li>최대 255자까지 가능
     * </ul>
     *
     * @example newuser@example.com
     */
    @NotBlank(message = "이메일은 필수 입력 항목입니다.") @Email(message = "유효한 이메일 형식이 아닙니다.") @Size(max = 255, message = "이메일은 최대 255자까지 가능합니다.") String email,

    /**
     * 사용자의 로그인 비밀번호
     *
     * <p>
     * 클라이언트에서 평문으로 전송되지만, 서버에서 BCrypt 알고리즘을 사용하여 안전하게 해시화되어 데이터베이스에 저장됨.
     *
     * <h3>보안 권장사항:</h3>
     *
     * <ul>
     * <li>최소 8자 이상, 최대 128자 이하
     * <li>영문 대소문자, 숫자, 특수문자 포함
     * <li>일반적인 사전 단어나 개인정보 사용 금지
     * </ul>
     *
     * @example securePassword123!
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.") @Size(min = 8, max = 128, message = "비밀번호는 8자 이상, 128자 이하이어야 합니다.") String password,

    /**
     * 사용자의 실명 또는 표시 이름
     *
     * <p>
     * 시스템 내에서 사용자를 식별하고 표시하는 데 사용되는 이름.
     *
     * <h3>제약사항:</h3>
     *
     * <ul>
     * <li>null이거나 빈 문자열이면 안됨
     * <li>최소 1자 이상, 최대 100자 이하
     * </ul>
     *
     * @example 홍길동
     */
    @NotBlank(message = "이름은 필수 입력 항목입니다.") @Size(min = 1, max = 100, message = "이름은 1자 이상, 100자 이하이어야 합니다.") String name) {

  /**
   * DTO를 User 엔티티로 변환하는 메서드 (v4.2)
   *
   * <p>
   * 이 메서드는 회원가입 요청 데이터를 실제 데이터베이스에 저장할 수 있는 User 엔티티 객체로 변환함.
   *
   * <h3>v4.2 변환 과정:</h3>
   *
   * <ol>
   * <li>이메일과 이름은 그대로 복사
   * <li>비밀번호는 BCrypt로 안전하게 암호화
   * <li>생성 시간과 수정 시간은 JPA에서 자동 설정
   * </ol>
   *
   * @param passwordEncoder 비밀번호 암호화를 위한 BCrypt 인코더
   * @return 데이터베이스 저장 준비가 완료된 User 엔티티 객체
   * @throws IllegalArgumentException passwordEncoder가 null인 경우
   * @see User.UserBuilder
   * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
   */
  public User toEntity(PasswordEncoder passwordEncoder) {
    if (passwordEncoder == null) {
      throw new IllegalArgumentException("PasswordEncoder는 null이 될 수 없음.");
    }

    return User.builder()
        .email(this.email)
        .passwordHash(passwordEncoder.encode(this.password)) // 비밀번호 암호화
        .name(this.name)
        .build();
  }
}
