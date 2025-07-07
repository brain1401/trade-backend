package com.hscoderadar.domain.notification.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@EntityListeners(AuditingEntityListener.class)
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
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  @Column(name = "notification_id")
  private String notificationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false)
  private NotificationType notificationType;

  @Column(name = "recipient", nullable = false)
  private String recipient; // 이메일 주소 또는 전화번호

  @Column(length = 500)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "external_message_id", length = 100)
  private String externalMessageId;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "cost_krw")
  private Integer costKrw;

  @Column(name = "scheduled_at")
  private LocalDateTime scheduledAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column
  @Builder.Default
  private boolean success = false;

  @Column(name = "message_type")
  private String messageType;

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