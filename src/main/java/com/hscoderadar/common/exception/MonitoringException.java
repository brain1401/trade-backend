package com.hscoderadar.common.exception;

/**
 * 모니터링 관련 예외를 나타내는 클래스
 * 
 * <p>
 * 이 예외는 모니터링 서비스 과정에서 발생할 수 있는 모든 오류를 처리합니다.
 * 변경 감지 실패, 북마크 처리 오류, 알림 발송 실패, 스케줄링 오류 등이 포함됩니다.
 * 
 * <h3>사용 예시:</h3>
 * 
 * <pre>{@code
 * // 모니터링 실패
 * throw new MonitoringException("변경 감지 서비스에서 오류가 발생했습니다");
 * 
 * // 스케줄링 오류 포함
 * throw new MonitoringException("모니터링 스케줄러 시작 실패", schedulerException);
 * }</pre>
 * 
 * @author Development Team
 * @since 1.0.0
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
public class MonitoringException extends RuntimeException {

  /**
   * 메시지만 포함하는 예외 생성
   * 
   * @param message 예외 메시지
   */
  public MonitoringException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인 예외를 포함하는 예외 생성
   * 
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public MonitoringException(String message, Throwable cause) {
    super(message, cause);
  }
}