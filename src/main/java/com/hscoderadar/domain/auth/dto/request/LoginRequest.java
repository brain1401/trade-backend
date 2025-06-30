package com.hscoderadar.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * AI 기반 무역 규제 레이더 플랫폼 v6.1 로그인 요청 DTO
 *
 * <p>
 * 🆕 v6.1 JWT 세부화 정책에 따른 로그인 요청 정보 - remember me 옵션으로 Refresh Token 수명 차별화 -
 * remember me 체크시:
 * 30일, 미체크시: 1일
 *
 * <p>
 * 📊 v6.1 응답 구조: - Access Token: 30분 고정 (JSON 응답) - Refresh Token: 1일/30일
 * (HttpOnly 쿠키)
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 */
public record LoginRequest(
    /**
     * 사용자 이메일 주소
     *
     * <p>
     * - 로그인 시 사용자 식별자로 사용 - 필수 입력 필드 - 이메일 형식 검증
     */
    @NotBlank(message = "이메일 입력 필수") @Email(message = "올바른 이메일 형식 입력 필요") String email,

    /**
     * 사용자 비밀번호
     *
     * <p>
     * - 필수 입력 필드 - 서버에서 BCrypt 해시와 비교
     */
    @NotBlank(message = "비밀번호 입력 필수") String password,

    /**
     * 🆕 v6.1 remember me 옵션 (JWT 세부화 핵심)
     *
     * <p>
     * Refresh Token 수명 차별화: - true: 30일 (편의성 우선) - false: 1일 (보안성 우선)
     *
     * <p>
     * 기본값: false (보안성 우선)
     */
    boolean rememberMe) {
}
