package com.hscoderadar.domain.chat.dto.response;

import com.hscoderadar.domain.chat.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답 DTO")
public record MessageResponse(
        @Schema(description = "메시지 ID", example = "1") Long messageId,
        @Schema(description = "메시지 타입 (USER, AI)", example = "USER") String messageType,
        @Schema(description = "메시지 내용") String content,
        @Schema(description = "메시지 생성 시간") LocalDateTime createdAt) {
    public static MessageResponse from(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt());
    }
}