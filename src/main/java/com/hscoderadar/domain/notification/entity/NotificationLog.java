package com.hscoderadar.domain.notification.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode; 
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(nullable = false)
    private String recipient; // 휴대폰 번호 또는 이메일

    @Column(length = 500)
    private String title; // 이메일 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "cost_krw")
    private Integer costKrw; // 발송 비용

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        SMS, EMAIL
    }

    public enum MessageType {
        VERIFICATION, DAILY_NOTIFICATION, URGENT_ALERT
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED
    }

    @Builder
    public NotificationLog(User user, NotificationType notificationType, MessageType messageType, String recipient, String title, String content) {
        this.user = user;
        this.notificationType = notificationType;
        this.messageType = messageType;
        this.recipient = recipient;
        this.title = title;
        this.content = content;
    }
    
    public void updateStatus(NotificationStatus status, String externalMessageId, String errorMessage) {
        this.status = status;
        this.externalMessageId = externalMessageId;
        this.errorMessage = errorMessage;
        if(status == NotificationStatus.SENT) {
            this.sentAt = LocalDateTime.now();
        }
    }
}