package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE 에러 이벤트
 * 채팅 처리 중 오류 발생 시 전송
 */
public record ErrorEvent(
    String message,
    String errorCode) {

  /**
   * 메시지만으로 에러 이벤트 생성
   */
  public ErrorEvent(String message) {
    this(message, null);
  }
}