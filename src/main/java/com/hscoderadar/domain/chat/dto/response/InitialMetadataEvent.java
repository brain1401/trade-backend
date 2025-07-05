package com.hscoderadar.domain.chat.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * SSE 초기 메타데이터 이벤트
 * 채팅 세션 시작 시 전송
 */
public record InitialMetadataEvent(
    @NotBlank(message = "요청 ID는 필수값") String requestId,

    @Positive(message = "타임스탬프는 양수여야 함") long timestamp,

    @NotBlank(message = "모델명은 필수값") String model) {
}