package com.hscoderadar.domain.chat.dto;

import com.hscoderadar.domain.chat.entity.ChatMessage;
import com.hscoderadar.domain.chat.entity.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "채팅 세션 상세 내역 응답 DTO")
public record ChatHistoryDetailResponse(
        SessionResponse sessionInfo,
        List<MessageResponse> messages
) {
    public static ChatHistoryDetailResponse of(ChatSession session, List<ChatMessage> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());

        return new ChatHistoryDetailResponse(SessionResponse.from(session), messageResponses);
    }
}