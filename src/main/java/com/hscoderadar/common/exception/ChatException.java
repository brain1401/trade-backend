package com.hscoderadar.common.exception;

import lombok.Getter;

/**
 * 채팅 시스템 관련 예외 클래스
 *
 * <p>v4.2에서 새롭게 추가된 단일 엔드포인트 채팅 시스템에서 발생할 수 있는 모든 오류를 처리. 메시지 검증 실패, Claude AI 분석 오류, 무역 외 질의 차단,
 * 스트리밍 오류 등이 포함됨.
 *
 * <h3>사용 예시:</h3>
 *
 * <pre>{@code
 * // 메시지 길이 검증 실패
 * throw ChatException.messageTooShort();
 *
 * // 무역 외 질의 차단
 * throw ChatException.nonTradeTopicBlocked();
 *
 * // Claude AI 분석 실패
 * throw ChatException.aiAnalysisFailed("API 호출 실패");
 * }</pre>
 *
 * @author HsCodeRadar Team
 * @since 4.2.0
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
@Getter
public class ChatException extends RuntimeException {

  private final ErrorCode errorCode;

  public ChatException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ChatException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  // 메시지 길이 부족
  public static ChatException messageTooShort() {
    return new ChatException(ErrorCode.CHAT_001);
  }

  // 무역 관련 외 질의 차단
  public static ChatException nonTradeTopicBlocked() {
    return new ChatException(ErrorCode.CHAT_002);
  }

  // Claude AI 분석 실패
  public static ChatException aiAnalysisFailed() {
    return new ChatException(ErrorCode.CHAT_003);
  }

  public static ChatException aiAnalysisFailed(Throwable cause) {
    return new ChatException(ErrorCode.CHAT_003, cause);
  }

  // Claude AI 응답 파싱 실패
  public static ChatException aiResponseParsingFailed() {
    return new ChatException(ErrorCode.CHAT_004);
  }

  public static ChatException aiResponseParsingFailed(Throwable cause) {
    return new ChatException(ErrorCode.CHAT_004, cause);
  }

  // 채팅 요청 한도 초과
  public static ChatException requestLimitExceeded() {
    return new ChatException(ErrorCode.CHAT_005);
  }
}
