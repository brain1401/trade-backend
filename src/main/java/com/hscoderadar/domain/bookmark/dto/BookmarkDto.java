package com.hscoderadar.domain.bookmark.dto;

import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.user.entity.User;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

public class BookmarkDto {

    @Getter
    @Builder
    public static class BookmarkResponse {
        private Long id;
        private Bookmark.BookmarkType type;
        private String targetValue;
        private String displayName;
        private boolean sseGenerated;
        private Map<String, Object> sseEventData;
        private boolean smsNotificationEnabled;
        private boolean emailNotificationEnabled;
        private boolean monitoringActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static BookmarkResponse from(Bookmark bookmark) {
            boolean isMonitoringActive = bookmark.isSmsNotificationEnabled() || bookmark.isEmailNotificationEnabled();
            
            Map<String, Object> sseEventDataMap = null;
            if (bookmark.getSseEventData() != null) {
                try {
                    sseEventDataMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(bookmark.getSseEventData(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                }
            }

            return BookmarkResponse.builder()
                .id(bookmark.getId())
                .type(bookmark.getType())
                .targetValue(bookmark.getTargetValue())
                .displayName(bookmark.getDisplayName())
                .sseGenerated(bookmark.isSseGenerated())
                .sseEventData(sseEventDataMap) 
                .smsNotificationEnabled(bookmark.isSmsNotificationEnabled())
                .emailNotificationEnabled(bookmark.isEmailNotificationEnabled())
                .monitoringActive(isMonitoringActive)
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
        private Object sseEventData;
        private boolean smsNotificationEnabled;
        private boolean emailNotificationEnabled;

        public Bookmark toEntity(User user) {
            return Bookmark.builder()
                .user(user)
                .type(type)
                .targetValue(targetValue)
                .displayName(displayName)
                .sseGenerated(sseGenerated)
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