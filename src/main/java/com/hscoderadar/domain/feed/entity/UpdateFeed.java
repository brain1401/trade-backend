package com.hscoderadar.domain.feed.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "update_feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UpdateFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false)
    private FeedType feedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "target_value", length = 50)
    private String targetValue; // 대상 HS Code 또는 화물관리번호

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportanceLevel importance = ImportanceLevel.MEDIUM;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "included_in_daily_notification", nullable = false)
    private boolean includedInDailyNotification = false;

    @Column(name = "daily_notification_sent_at")
    private LocalDateTime dailyNotificationSentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FeedType {
        HS_CODE_TARIFF_CHANGE, HS_CODE_REGULATION_UPDATE, CARGO_STATUS_UPDATE, TRADE_NEWS, POLICY_UPDATE
    }

    public enum TargetType {
        HS_CODE, CARGO
    }

    public enum ImportanceLevel {
        HIGH, MEDIUM, LOW
    }

    @Builder
    public UpdateFeed(User user, FeedType feedType, TargetType targetType, String targetValue, String title, String content, String sourceUrl, ImportanceLevel importance) {
        this.user = user;
        this.feedType = feedType;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.importance = importance;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsIncludedInNotification(LocalDateTime sentAt) {
        this.includedInDailyNotification = true;
        this.dailyNotificationSentAt = sentAt;
    }
}