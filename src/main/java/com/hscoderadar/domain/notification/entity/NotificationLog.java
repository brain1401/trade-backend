package com.hscoderadar.domain.notification.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 발송 로그 엔티티
 * 모든 알림 발송 기록을 추적
 */
@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

  /**
   * 알림 타입 열거형
   */
  @Getter
  public enum NotificationType {
    EMAIL("이메일"),
    SMS("SMS");

    private final String description;

    NotificationType(String description) {
      this.description = description;
    }
  }

  /**
   * 알림 상태 열거형
   */
  @Getter
  public enum NotificationStatus {
    SENT("발송 완료"),
    FAILED("발송 실패"),
    PENDING("발송 대기중");

    private final String description;

    NotificationStatus(String description) {
      this.description = description;
    }
  }

  /**
   * 메시지 타입 열거형
   */
  @Getter
  public enum MessageType {
    DAILY_NOTIFICATION("일일 알림"),
    INSTANT_NOTIFICATION("즉시 알림"),
    SCHEDULED_NOTIFICATION("예약 알림");

    private final String description;

    MessageType(String description) {
      this.description = description;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "notification_id", length = 36)
  private String taskId; // Redis 큐에서 사용한 작업 ID

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", length = 20, nullable = false)
  private NotificationType notificationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", length = 30, nullable = false)
  private MessageType messageType;

  @Column(name = "recipient", length = 255, nullable = false)
  private String recipient; // 이메일 주소 또는 전화번호

  @Column(name = "title", length = 255)
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "success", nullable = false)
  @Builder.Default
  private boolean success = false;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (status == null) {
      status = NotificationStatus.PENDING;
    }
  }

  /**
   * 알림 상태 업데이트
   * 
   * @param status       새로운 상태
   * @param sentAt       발송 시간
   * @param errorMessage 에러 메시지 (실패 시)
   */
  public void updateStatus(NotificationStatus status, LocalDateTime sentAt, String errorMessage) {
    this.status = status;
    this.success = (status == NotificationStatus.SENT);
    this.sentAt = sentAt != null ? sentAt : LocalDateTime.now();
    this.errorMessage = errorMessage;
  }
}