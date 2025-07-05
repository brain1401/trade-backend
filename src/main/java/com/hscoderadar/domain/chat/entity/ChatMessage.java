package com.hscoderadar.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

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

  @Column(name = "session_uuid", nullable = false)
  private UUID sessionUuid;

  @Column(name = "session_created_at", nullable = false)
  private LocalDateTime sessionCreatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "session_uuid", referencedColumnName = "session_uuid", insertable = false, updatable = false),
      @JoinColumn(name = "session_created_at", referencedColumnName = "created_at", insertable = false, updatable = false)
  })
  private ChatSession session;

  @Column(name = "message_type", nullable = false)
  private String messageType;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "ai_model")
  private String aiModel;

  @Column(name = "thinking_process", columnDefinition = "TEXT")
  private String thinkingProcess;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "hscode_analysis", columnDefinition = "jsonb")
  private Object hscodeAnalysis;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sse_bookmark_data", columnDefinition = "jsonb")
  private Object sseBookmarkData;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}