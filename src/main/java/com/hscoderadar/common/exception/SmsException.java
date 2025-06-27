package com.hscoderadar.common.exception;

import lombok.Getter;

/**
 * SMS 알림 시스템 관련 예외 클래스
 *
 * <p>v4.2에서 강화된 SMS 알림 시스템에서 발생할 수 있는 모든 오류를 처리. 휴대폰 인증, SMS 발송, 알림 설정 변경 등의 과정에서 발생하는 다양한 예외 상황을
 * 포함.
 *
 * <h3>사용 예시:</h3>
 *
 * <pre>{@code
 * // 휴대폰 번호 형식 오류
 * throw SmsException.invalidPhoneNumber();
 *
 * // 인증 코드 발송 한도 초과
 * throw SmsException.verificationLimitExceeded();
 *
 * // SMS 발송 실패
 * throw SmsException.sendFailed("외부 SMS API 오류");
 * }</pre>
 *
 * @author HsCodeRadar Team
 * @since 4.2.0
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
@Getter
public class SmsException extends RuntimeException {

  private final ErrorCode errorCode;

  public SmsException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public SmsException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  // 휴대폰 번호 형식 오류
  public static SmsException invalidPhoneNumber() {
    return new SmsException(ErrorCode.SMS_001);
  }

  // 이미 인증된 휴대폰 번호
  public static SmsException phoneAlreadyVerified() {
    return new SmsException(ErrorCode.SMS_002);
  }

  // 인증 코드 발송 한도 초과
  public static SmsException verificationLimitExceeded() {
    return new SmsException(ErrorCode.SMS_003);
  }

  // 잘못된 인증 코드
  public static SmsException invalidVerificationCode() {
    return new SmsException(ErrorCode.SMS_004);
  }

  // 인증 코드 만료
  public static SmsException verificationCodeExpired() {
    return new SmsException(ErrorCode.SMS_005);
  }

  // SMS 발송 실패
  public static SmsException sendFailed() {
    return new SmsException(ErrorCode.SMS_006);
  }

  public static SmsException sendFailed(Throwable cause) {
    return new SmsException(ErrorCode.SMS_006, cause);
  }

  // 휴대폰 인증 필요함
  public static SmsException phoneVerificationRequired() {
    return new SmsException(ErrorCode.SMS_007);
  }

  // SMS 알림 설정 변경 권한 없음
  public static SmsException settingsChangeNotAllowed() {
    return new SmsException(ErrorCode.SMS_008);
  }
}
