package com.hscoderadar.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 * 개별 채팅 메시지 저장
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private ChatSession session;

  @Column(name = "user_message", columnDefinition = "TEXT", nullable = false)
  private String userMessage;

  @Column(name = "ai_response", columnDefinition = "TEXT")
  private String aiResponse;

  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;

  @PrePersist
  protected void onCreate() {
    if (timestamp == null) {
      timestamp = LocalDateTime.now();
    }
  }
}