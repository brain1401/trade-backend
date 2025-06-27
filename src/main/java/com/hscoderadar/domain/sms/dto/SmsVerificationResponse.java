package com.hscoderadar.domain.sms.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * SMS 인증 코드 발송 응답 DTO
 */
@Getter
@Builder
public class SmsVerificationResponse {

  /**
   * 인증 세션 ID
   */
  private String verificationId;

  /**
   * 인증 코드 만료 시간 (ISO 8601)
   */
  private String expiresAt;

  /**
   * 다음 발송 가능 시간 (ISO 8601)
   */
  private String cooldownUntil;
}