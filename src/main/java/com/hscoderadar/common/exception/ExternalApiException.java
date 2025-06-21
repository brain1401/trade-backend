package com.hscoderadar.common.exception;

/**
 * 외부 API 호출 실패 예외를 나타내는 클래스
 * 
 * <p>
 * 이 예외는 외부 API(한국 관세청 API, UN COMTRADE API 등) 호출 과정에서
 * 발생할 수 있는 모든 오류를 처리합니다. 네트워크 오류, 인증 실패,
 * 응답 파싱 오류, 서비스 일시 중단 등이 포함됩니다.
 * 
 * <h3>사용 예시:</h3>
 * 
 * <pre>{@code
 * // API 호출 실패
 * throw new ExternalApiException("관세청 API 서비스가 일시적으로 중단되었습니다");
 * 
 * // 네트워크 오류 포함
 * throw new ExternalApiException("UN COMTRADE API 연결 실패", networkException);
 * }</pre>
 * 
 * @author Development Team
 * @since 1.0.0
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
public class ExternalApiException extends RuntimeException {

  /**
   * 메시지만 포함하는 예외 생성
   * 
   * @param message 예외 메시지
   */
  public ExternalApiException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인 예외를 포함하는 예외 생성
   * 
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public ExternalApiException(String message, Throwable cause) {
    super(message, cause);
  }
}