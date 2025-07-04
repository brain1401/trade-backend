package com.hscoderadar.domain.chat.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 세션 엔티티
 * 사용자별 채팅 대화 세션 관리
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

  @Id
  @Column(name = "session_id", length = 36)
  private String sessionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_activity_at", nullable = false)
  private LocalDateTime lastActivityAt;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ChatMessage> messages = new ArrayList<>();

  /**
   * 메시지 추가 헬퍼 메소드
   */
  public void addMessage(ChatMessage message) {
    messages.add(message);
    message.setSession(this);
    this.lastActivityAt = LocalDateTime.now();
  }

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (lastActivityAt == null) {
      lastActivityAt = LocalDateTime.now();
    }
  }
}