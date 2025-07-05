package com.hscoderadar.domain.chat.service;

import com.hscoderadar.domain.chat.dto.response.ChatHistoryDetailResponse;
import com.hscoderadar.domain.chat.dto.response.SessionResponse;
import com.hscoderadar.domain.chat.entity.ChatMessage;
import com.hscoderadar.domain.chat.entity.ChatSession;
import com.hscoderadar.domain.chat.repository.ChatMessageRepository;
import com.hscoderadar.domain.chat.repository.ChatSessionRepository;
import com.hscoderadar.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatHistoryService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public Page<SessionResponse> getChatSessions(User user, Pageable pageable) {
        Page<ChatSession> sessions = chatSessionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return sessions.map(SessionResponse::from);
    }

    public ChatHistoryDetailResponse getChatHistoryDetail(User user, UUID sessionId) {
        // 1. 세션 정보 조회 및 소유권 확인
        ChatSession session = chatSessionRepository.findBySessionUuid(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("채팅 세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 채팅 기록에 접근할 권한이 없습니다.");
        }

        // 2. 해당 세션의 모든 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findBySessionUuidOrderByCreatedAtAsc(sessionId);

        // 3. DTO로 변환하여 반환
        return ChatHistoryDetailResponse.of(session, messages);
    }
}