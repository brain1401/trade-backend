package com.hscoderadar.domain.chat.dto.response;

import com.hscoderadar.domain.chat.entity.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "채팅 세션 목록의 개별 항목 응답 DTO")
public record SessionResponse(
    @Schema(description = "세션 UUID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") UUID sessionId,
    @Schema(description = "세션 제목 (첫 질문 요약)", example = "아이폰 15 프로 HS Code 문의") String sessionTitle,
    @Schema(description = "세션 내 메시지 수", example = "6") int messageCount,
    @Schema(description = "세션 생성 시간") OffsetDateTime createdAt,
    @Schema(description = "세션 마지막 업데이트 시간") OffsetDateTime updatedAt) {
  public static SessionResponse from(ChatSession session) {
    return new SessionResponse(
        session.getSessionUuid(),
        session.getSessionTitle(),
        session.getMessageCount(),
        session.getCreatedAt(),
        session.getUpdatedAt());
  }
}