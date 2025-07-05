package com.hscoderadar.domain.chat.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

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
@IdClass(ChatSession.ChatSessionId.class)
public class ChatSession {

  @Id
  @Column(name = "session_uuid")
  private UUID sessionUuid;

  @Id
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "session_title")
  private String sessionTitle;

  @Column(name = "message_count")
  @Builder.Default
  private Integer messageCount = 0;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
  }

  /**
   * 복합 키 클래스
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChatSessionId implements Serializable {
    private UUID sessionUuid;
    private LocalDateTime createdAt;
  }
}