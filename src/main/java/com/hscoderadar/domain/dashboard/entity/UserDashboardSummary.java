package com.hscoderadar.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import java.time.LocalTime;

/**
 * v_user_dashboard_summary 뷰와 매핑되는 읽기 전용 엔티티
 */
@Entity
@Immutable
@Table(name = "v_user_dashboard_summary")
@Getter
public class UserDashboardSummary {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "total_bookmarks")
    private Long totalBookmarks;

    @Column(name = "active_monitoring")
    private Long activeMonitoring;

    @Column(name = "sse_generated_bookmarks")
    private Long sseGeneratedBookmarks;

    @Column(name = "unread_feeds")
    private Long unreadFeeds;

    @Column(name = "high_importance_feeds")
    private Long highImportanceFeeds;
    
    @Column(name = "total_chat_sessions")
    private Long totalChatSessions;

    @Column(name = "recent_chat_sessions_30d")
    private Long recentChatSessions30d;

    @Column(name = "total_chat_messages")
    private Long totalChatMessages;
    
    @Column(name = "has_valid_refresh_token")
    private Boolean hasValidRefreshToken;

    @Column(name = "remember_me_enabled")
    private Boolean rememberMeEnabled;

    @Column(name = "sms_notification_enabled")
    private Boolean smsNotificationEnabled;

    @Column(name = "email_notification_enabled")
    private Boolean emailNotificationEnabled;

    @Column(name = "notification_time")
    private LocalTime notificationTime;
}