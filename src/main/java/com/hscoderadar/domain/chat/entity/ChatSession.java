package com.hscoderadar.domain.chat.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(ChatSessionId.class)
public class ChatSession {

    @Id
    @Column(name = "session_uuid", nullable = false)
    private UUID sessionUuid;

    @Id
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_title", length = 255)
    private String sessionTitle;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public ChatSession(User user) {
        this.sessionUuid = UUID.randomUUID();
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}