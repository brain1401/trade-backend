package com.hscoderadar.domain.dashboard.dto;

import com.hscoderadar.domain.dashboard.entity.UserDashboardSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record DashboardDto() {

    @Schema(description = "대시보드 요약 정보 응답 DTO")
    public record DashboardSummaryResponse(
        UserSummary user,
        BookmarkSummary bookmarks,
        ChatSummary chatHistory,
        NotificationSummary notifications
    ) {
        public static DashboardSummaryResponse from(UserDashboardSummary summary) {
            return new DashboardSummaryResponse(
                new UserSummary(
                    summary.getUserName(),
                    summary.getEmail(),
                    summary.getPhoneVerified(),
                    summary.getRememberMeEnabled()
                ),
                new BookmarkSummary(
                    summary.getTotalBookmarks(),
                    summary.getActiveMonitoring(),
                    summary.getSseGeneratedBookmarks()
                ),
                new ChatSummary(
                    summary.getTotalChatSessions(),
                    summary.getRecentChatSessions30d(),
                    summary.getTotalChatMessages()
                ),
                new NotificationSummary(
                    summary.getUnreadFeeds(),
                    summary.getHighImportanceFeeds(),
                    summary.getSmsNotificationEnabled(),
                    summary.getEmailNotificationEnabled(),
                    summary.getNotificationTime()
                )
            );
        }
    }

    @Schema(description = "사용자 요약 정보")
    public record UserSummary(String name, String email, boolean phoneVerified, boolean rememberMe) {}

    @Schema(description = "북마크 통계")
    public record BookmarkSummary(long total, long activeMonitoring, long sseGenerated) {}
    
    @Schema(description = "채팅 기록 통계")
    public record ChatSummary(long totalSessions, long recentSessions30d, long totalMessages) {}

    @Schema(description = "알림 통계")
    public record NotificationSummary(
        long unreadFeeds,
        long highImportanceFeeds,
        boolean smsEnabled,
        boolean emailEnabled,
        LocalTime notificationTime
    ) {}
}