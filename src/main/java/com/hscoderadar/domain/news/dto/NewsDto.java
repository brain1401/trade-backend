package com.hscoderadar.domain.news.dto;

import com.hscoderadar.domain.tradenews.entity.TradeNewsCache;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NewsDto {
    private final Long id;
    private final String title;
    private final String summary;
    private final String sourceName;
    private final String sourceUrl;
    private final LocalDateTime publishedAt;
    private final String category;

    @Builder
    private NewsDto(Long id, String title, String summary, String sourceName, String sourceUrl, LocalDateTime publishedAt, String category) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.category = category;
    }

    public static NewsDto from(TradeNewsCache entity) {
        return NewsDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .sourceName(entity.getSourceName())
                .sourceUrl(entity.getSourceUrl())
                .publishedAt(entity.getPublishedAt())
                .category(entity.getCategory())
                .build();
    }
}