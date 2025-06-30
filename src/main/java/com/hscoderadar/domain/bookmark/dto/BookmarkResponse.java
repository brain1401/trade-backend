package com.hscoderadar.domain.bookmark.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.domain.bookmark.entity.Bookmark;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record BookmarkResponse(
    Long id,
    Bookmark.BookmarkType type,
    String targetValue,
    String displayName,
    boolean sseGenerated,
    Map<String, Object> sseEventData,
    boolean smsNotificationEnabled,
    boolean emailNotificationEnabled,
    boolean monitoringActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static BookmarkResponse from(Bookmark bookmark) {
    boolean isMonitoringActive = bookmark.isSmsNotificationEnabled() || bookmark.isEmailNotificationEnabled();

    Map<String, Object> sseEventDataMap = null;
    if (bookmark.getSseEventData() != null) {
      try {
        sseEventDataMap = objectMapper.readValue(bookmark.getSseEventData(), new TypeReference<>() {
        });
      } catch (IOException e) {
        log.error(
            "Failed to parse sseEventData JSON string: {}", bookmark.getSseEventData(), e);
      }
    }

    return new BookmarkResponse(
        bookmark.getId(),
        bookmark.getType(),
        bookmark.getTargetValue(),
        bookmark.getDisplayName(),
        bookmark.isSseGenerated(),
        sseEventDataMap,
        bookmark.isSmsNotificationEnabled(),
        bookmark.isEmailNotificationEnabled(),
        isMonitoringActive,
        bookmark.getCreatedAt(),
        bookmark.getUpdatedAt());
  }
}