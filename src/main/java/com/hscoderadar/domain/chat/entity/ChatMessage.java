package com.hscoderadar.domain.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "session_uuid", nullable = false)
    private UUID sessionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "session_uuid", referencedColumnName = "session_uuid", insertable = false, updatable = false),
        @JoinColumn(name = "session_created_at", referencedColumnName = "created_at", insertable = false, updatable = false)
    })
    private ChatSession chatSession;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(ChatSession chatSession, String messageType, String content) {
        this.chatSession = chatSession;
        this.sessionUuid = chatSession.getSessionUuid();
        this.messageType = messageType;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}