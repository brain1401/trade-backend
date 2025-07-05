package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE 세션 정보 이벤트
 * 채팅 세션 ID와 신규 세션 여부 전달
 */
public record SessionInfoEvent(
    String sessionId,
    boolean isNewSession) {
}