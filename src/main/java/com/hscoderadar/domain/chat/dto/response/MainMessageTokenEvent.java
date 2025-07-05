package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE 메인 메시지 토큰 이벤트
 * AI 응답을 스트리밍할 때 각 토큰별로 전송됨
 */
public record MainMessageTokenEvent(
    String token) {
}