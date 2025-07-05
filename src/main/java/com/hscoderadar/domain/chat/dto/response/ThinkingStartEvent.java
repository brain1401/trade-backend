package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE Thinking 시작 이벤트
 * AI가 응답 생성을 시작할 때 전송됨
 */
public record ThinkingStartEvent(
    long timestamp) {
}