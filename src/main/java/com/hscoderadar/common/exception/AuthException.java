package com.hscoderadar.common.exception;

import lombok.Getter;

/**
 * 인증 관련 비즈니스 예외 클래스
 * 
 * 사용자 열거 공격 방지를 위해 모든 인증 실패는 AUTH_001로 통일하여 처리
 * v4.2에서 Spring Session 기반 세션 관리 지원 추가
 * 
 * @author HsCodeRadar Team
 * @since 4.2.0
 */
@Getter
public class AuthException extends RuntimeException {

  private final ErrorCode errorCode;

  public AuthException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AuthException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  // 사용자 열거 공격 방지를 위한 통합 인증 실패 예외
  public static AuthException invalidCredentials() {
    return new AuthException(ErrorCode.AUTH_001);
  }

  // 계정 잠김
  public static AuthException accountLocked() {
    return new AuthException(ErrorCode.AUTH_002);
  }

  // 토큰 만료 (기존 호환성 유지)
  public static AuthException tokenExpired() {
    return new AuthException(ErrorCode.AUTH_003);
  }

  // 인증 정보 오류 (기존 호환성 유지)
  public static AuthException invalidToken() {
    return new AuthException(ErrorCode.AUTH_004);
  }

  // 접근 권한 없음
  public static AuthException accessDenied() {
    return new AuthException(ErrorCode.AUTH_005);
  }

  // v4.2 추가: Spring Session 관련 예외들

  // 세션 만료
  public static AuthException sessionExpired() {
    return new AuthException(ErrorCode.AUTH_006);
  }

  // 세션 정보 오류
  public static AuthException sessionInvalid() {
    return new AuthException(ErrorCode.AUTH_007);
  }

  // 세션 없음
  public static AuthException sessionNotFound() {
    return new AuthException(ErrorCode.AUTH_008);
  }

  // 🆕 v6.1 추가: JWT 세부화 정책 관련 예외들

  // 사용자를 찾을 수 없음
  public static AuthException userNotFound() {
    return new AuthException(ErrorCode.AUTH_001); // 사용자 열거 공격 방지를 위해 AUTH_001 사용
  }

  // 토큰 갱신 실패
  public static AuthException tokenRefreshFailed() {
    return new AuthException(ErrorCode.AUTH_004); // 토큰 관련 오류로 분류
  }

  // 리프레시 토큰 불일치
  public static AuthException refreshTokenMismatch() {
    return new AuthException(ErrorCode.AUTH_004); // 토큰 관련 오류로 분류
  }

  // 리프레시 토큰 만료
  public static AuthException refreshTokenExpired() {
    return new AuthException(ErrorCode.AUTH_003); // 토큰 만료 오류로 분류
  }

  // 토큰 블랙리스트 오류
  public static AuthException tokenBlacklisted() {
    return new AuthException(ErrorCode.AUTH_004); // 토큰 관련 오류로 분류
  }
}