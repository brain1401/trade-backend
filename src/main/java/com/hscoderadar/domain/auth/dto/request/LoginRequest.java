package com.hscoderadar.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 로그인 요청을 위한 데이터 전송 객체 (DTO)
 * 
 * <p>
 * JWT 기반 인증 시스템에서 사용자의 로그인 정보를 전달하는 데 사용됩니다.
 * HttpOnly 쿠키 기반 JWT 토큰 관리를 지원하며, Remember Me 기능을 포함합니다.
 * 
 * <h3>사용 예시:</h3>
 * 
 * <pre>{@code
 * LoginRequest request = new LoginRequest(
 *         "user@example.com",
 *         "password123",
 *         true // Remember Me
 * );
 * }</pre>
 * 
 * <h3>Remember Me 기능:</h3>
 * <ul>
 * <li>true: JWT 쿠키 수명 7일 (Max-Age=604800)</li>
 * <li>false: 세션 쿠키 (브라우저 종료 시 삭제)</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 사용자 이메일 주소
     * 
     * <p>
     * 로그인 식별자로 사용되며, 유효한 이메일 형식이어야 합니다.
     * 대소문자를 구분하지 않으며, 데이터베이스 조회 시 자동으로 소문자로 변환됩니다.
     * 
     * <h3>유효성 검증:</h3>
     * <ul>
     * <li>필수 입력 (@NotBlank)</li>
     * <li>유효한 이메일 형식 (@Email)</li>
     * <li>최대 255자까지 허용</li>
     * </ul>
     * 
     * @example user@example.com
     */
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식을 입력해주세요.")
    private String email;

    /**
     * 사용자 비밀번호
     * 
     * <p>
     * 사용자의 인증용 비밀번호입니다. 서버에서 BCrypt 해시와 비교하여 인증을 수행합니다.
     * 보안상 클라이언트에서 평문으로 전송되지만, HTTPS를 통해 암호화되어 전송됩니다.
     * 
     * <h3>보안 특징:</h3>
     * <ul>
     * <li>서버에서 BCrypt로 해시 비교</li>
     * <li>로그에 기록되지 않음</li>
     * <li>메모리에서 처리 후 즉시 제거</li>
     * </ul>
     * 
     * <h3>유효성 검증:</h3>
     * <ul>
     * <li>필수 입력 (@NotBlank)</li>
     * <li>최소 8자 이상 권장 (프론트엔드에서 검증)</li>
     * </ul>
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;

    /**
     * 로그인 유지 여부 (Remember Me)
     * 
     * <p>
     * 사용자의 로그인 상태를 장기간 유지할지 여부를 결정합니다.
     * 이 설정에 따라 JWT 쿠키의 만료 시간이 결정됩니다.
     * 
     * <h3>쿠키 수명 설정:</h3>
     * <ul>
     * <li><strong>true:</strong> 영구 쿠키 (7일간 유지, Max-Age=604800)</li>
     * <li><strong>false:</strong> 세션 쿠키 (브라우저 종료 시 삭제)</li>
     * </ul>
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>공용 컴퓨터에서는 false 권장</li>
     * <li>개인 디바이스에서는 true로 편의성 제공</li>
     * <li>JWT 만료 시간과 별개로 쿠키 수명만 결정</li>
     * </ul>
     * 
     * @default false
     */
    private boolean rememberMe = false;

    /**
     * 이메일과 비밀번호만으로 로그인 요청 객체를 생성합니다.
     * 
     * <p>
     * Remember Me는 기본값 false로 설정되어 세션 쿠키로 동작합니다.
     * 
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.rememberMe = false;
    }
}