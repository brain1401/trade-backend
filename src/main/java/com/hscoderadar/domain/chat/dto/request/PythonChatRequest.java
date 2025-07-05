package com.hscoderadar.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Python AI 서버로 보내는 채팅 요청 DTO
 * Python 서버 API 명세에 따라 snake_case 사용
 */
public record PythonChatRequest(
    @JsonProperty("user_id") String userId,

    @JsonProperty("session_uuid") String sessionUuid,

    @NotBlank String message) {
}