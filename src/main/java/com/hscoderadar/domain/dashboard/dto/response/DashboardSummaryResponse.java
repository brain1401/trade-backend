package com.hscoderadar.domain.dashboard.dto.response;

import com.hscoderadar.domain.dashboard.entity.UserDashboardSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대시보드 요약 정보 응답 DTO")
public record DashboardSummaryResponse(
    UserSummary user,
    BookmarkSummary bookmarks,
    ChatSummary chatHistory,
    NotificationSummary notifications) {
  public static DashboardSummaryResponse from(UserDashboardSummary summary) {
    return new DashboardSummaryResponse(
        new UserSummary(
            summary.getUserName(),
            summary.getEmail(),
            summary.getPhoneVerified(),
            summary.getRememberMeEnabled()),
        new BookmarkSummary(
            summary.getTotalBookmarks(),
            summary.getActiveMonitoring(),
            summary.getSseGeneratedBookmarks()),
        new ChatSummary(
            summary.getTotalChatSessions(),
            summary.getRecentChatSessions30d(),
            summary.getTotalChatMessages()),
        new NotificationSummary(
            summary.getUnreadFeeds(),
            summary.getHighImportanceFeeds(),
            summary.getSmsNotificationEnabled(),
            summary.getEmailNotificationEnabled(),
            summary.getNotificationTime()));
  }
}