package com.hscoderadar.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프론트엔드로부터 받는 채팅 요청 DTO
 */
public record ChatRequest(
    @NotBlank(message = "메시지는 필수 입력값임") @Size(max = 4000, message = "메시지는 4000자를 초과할 수 없음") String message,

    @JsonProperty("session_uuid") String sessionUuid) {
}