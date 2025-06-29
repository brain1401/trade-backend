package com.hscoderadar.common.exception;

import lombok.Getter;

/**
 * Rate Limiting 관련 예외 클래스
 *
 * @author HsCodeRadar Team
 * @since 4.2.0
 */
@Getter
public class RateLimitException extends RuntimeException {

  private final ErrorCode errorCode;

  public RateLimitException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  // 로그인 시도 한도 초과
  public static RateLimitException loginAttemptsExceeded() {
    return new RateLimitException(ErrorCode.RATE_LIMIT_001);
  }

  // 검색 요청 한도 초과
  public static RateLimitException searchRequestsExceeded() {
    return new RateLimitException(ErrorCode.RATE_LIMIT_002);
  }

  // API 호출 한도 초과 (v4.2 추가)
  public static RateLimitException apiCallsExceeded() {
    return new RateLimitException(ErrorCode.RATE_LIMIT_003);
  }
}
