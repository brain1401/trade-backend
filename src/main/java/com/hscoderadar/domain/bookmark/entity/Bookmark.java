package com.hscoderadar.domain.bookmark.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookmarkType type;

    @Column(name = "target_value", nullable = false, length = 50)
    private String targetValue; // HS Code 또는 화물관리번호

    @Column(name = "display_name", length = 200)
    private String displayName; // 사용자 지정 표시명

    @Column(name = "sse_generated", nullable = false)
    private boolean sseGenerated = false; // SSE 이벤트로 생성된 북마크 여부

    @Column(name = "sse_event_data", columnDefinition = "JSONB")
    private String sseEventData; // SSE 이벤트 생성 시 전달된 데이터

    @Column(name = "sms_notification_enabled", nullable = false)
    private boolean smsNotificationEnabled = false;

    @Column(name = "email_notification_enabled", nullable = false)
    private boolean emailNotificationEnabled = true;

    @Column(name = "monitoring_active", insertable = false, updatable = false)
    private Boolean monitoringActive; // 알림 설정 기반 자동 계산

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BookmarkType {
        HS_CODE, CARGO
    }

    @Builder
    public Bookmark(User user, BookmarkType type, String targetValue, String displayName, boolean sseGenerated, String sseEventData, boolean smsNotificationEnabled, boolean emailNotificationEnabled) {
        this.user = user;
        this.type = type;
        this.targetValue = targetValue;
        this.displayName = displayName;
        this.sseGenerated = sseGenerated;
        this.sseEventData = sseEventData;
        this.smsNotificationEnabled = smsNotificationEnabled;
        this.emailNotificationEnabled = emailNotificationEnabled;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateNotificationSettings(boolean smsEnabled, boolean emailEnabled) {
        this.smsNotificationEnabled = smsEnabled;
        this.emailNotificationEnabled = emailEnabled;
    }
}