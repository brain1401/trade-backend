package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE 상세 페이지 버튼 준비 이벤트
 * AI 응답 완료 후 상세 페이지 버튼 활성화를 위해 전송됨
 */
public record DetailPageButtonReadyEvent(
    String sessionId,
    String buttonText,
    String buttonAction) {
}