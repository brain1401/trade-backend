package com.hscoderadar.domain.chat.dto.response;

/**
 * Python AI 서버로부터 받는 SSE 이벤트
 */
public record PythonSseEvent(
    String type,
    String data) {
}