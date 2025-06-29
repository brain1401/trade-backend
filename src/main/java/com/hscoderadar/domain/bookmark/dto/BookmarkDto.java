package com.hscoderadar.domain.bookmark.dto;

import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.user.entity.User;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class BookmarkDto {

    @Getter
    @Builder
    public static class BookmarkResponse {
        private Long id;
        private Bookmark.BookmarkType type;
        private String targetValue;
        private String displayName;
        private boolean sseGenerated;
        private boolean smsNotificationEnabled;
        private boolean emailNotificationEnabled;
        private boolean monitoringActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static BookmarkResponse from(Bookmark bookmark) {
            boolean isMonitoringActive = bookmark.isSmsNotificationEnabled() || bookmark.isEmailNotificationEnabled();
            
            return BookmarkResponse.builder()
                .id(bookmark.getId())
                .type(bookmark.getType())
                .targetValue(bookmark.getTargetValue())
                .displayName(bookmark.getDisplayName())
                .sseGenerated(bookmark.isSseGenerated())
                .smsNotificationEnabled(bookmark.isSmsNotificationEnabled())
                .emailNotificationEnabled(bookmark.isEmailNotificationEnabled())
                .monitoringActive(isMonitoringActive) // DB에서 가져오는 대신 직접 계산한 값을 사용
                .createdAt(bookmark.getCreatedAt())
                .updatedAt(bookmark.getUpdatedAt())
                .build();
        }
    }

    @Getter
    public static class BookmarkCreateRequest {
        private Bookmark.BookmarkType type;
        private String targetValue;
        private String displayName;
        private boolean sseGenerated;
        private String sseEventData;
        private boolean smsNotificationEnabled;
        private boolean emailNotificationEnabled;

        public Bookmark toEntity(User user) {
            return Bookmark.builder()
                .user(user)
                .type(type)
                .targetValue(targetValue)
                .displayName(displayName)
                .sseGenerated(sseGenerated)
                .sseEventData(sseEventData)
                .smsNotificationEnabled(smsNotificationEnabled)
                .emailNotificationEnabled(emailNotificationEnabled)
                .build();
        }
    }

    @Getter
    public static class BookmarkUpdateRequest {
        private String displayName;
        private Boolean smsNotificationEnabled;
        private Boolean emailNotificationEnabled;
    }
}