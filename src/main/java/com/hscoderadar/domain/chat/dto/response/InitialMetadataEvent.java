package com.hscoderadar.domain.chat.dto.response;

/**
 * SSE 초기 메타데이터 이벤트
 * 채팅 세션 시작 시 전송됨
 */
public record InitialMetadataEvent(
    String requestId,
    long timestamp,
    String model) {
}