package com.hscoderadar.domain.feed.dto;

import com.hscoderadar.domain.feed.entity.UpdateFeed;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FeedDto() {

    @Schema(description = "업데이트 피드 응답 DTO")
    public record FeedResponse(
        Long id,
        String feedType,
        String targetType,
        String targetValue,
        String title,
        String content,
        String sourceUrl,
        String importance,
        boolean isRead,
        LocalDateTime createdAt
    ) {
        public static FeedResponse from(UpdateFeed feed) {
            return new FeedResponse(
                feed.getId(),
                feed.getFeedType().name(),
                feed.getTargetType() != null ? feed.getTargetType().name() : null,
                feed.getTargetValue(),
                feed.getTitle(),
                feed.getContent(),
                feed.getSourceUrl(),
                feed.getImportance().name(),
                feed.isRead(),
                feed.getCreatedAt()
            );
        }
    }
}