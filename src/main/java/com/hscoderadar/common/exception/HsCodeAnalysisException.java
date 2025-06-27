package com.hscoderadar.common.exception;

/**
 * HS Code 분석 관련 예외를 나타내는 클래스
 *
 * <p>이 예외는 HS Code 분석 과정에서 발생할 수 있는 모든 오류를 처리. AI 모델 호출 실패, 분석 결과 파싱 오류, 외부 API 연동 실패 등이 포함됨.
 *
 * <h3>사용 예시:</h3>
 *
 * <pre>{@code
 * // AI 분석 실패
 * throw new HsCodeAnalysisException("AI 모델 응답을 파싱할 수 없음");
 *
 * // 외부 원인 포함
 * throw new HsCodeAnalysisException("Claude API 호출 실패", originalException);
 * }</pre>
 *
 * @author Development Team
 * @since 4.2.0
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
public class HsCodeAnalysisException extends RuntimeException {

  /**
   * 메시지만 포함하는 예외 생성
   *
   * @param message 예외 메시지
   */
  public HsCodeAnalysisException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인 예외를 포함하는 예외 생성
   *
   * @param message 예외 메시지
   * @param cause 원인 예외
   */
  public HsCodeAnalysisException(String message, Throwable cause) {
    super(message, cause);
  }
}
