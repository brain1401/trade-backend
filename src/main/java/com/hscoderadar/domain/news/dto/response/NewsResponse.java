package com.hscoderadar.domain.news.dto.response;

import com.hscoderadar.domain.tradenews.entity.TradeNewsCache;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "뉴스 정보 응답 DTO")
public record NewsResponse(
    Long id,
    String title,
    String summary,
    String sourceName,
    String sourceUrl,
    LocalDateTime publishedAt,
    String category) {

  public static NewsResponse from(TradeNewsCache entity) {
    return new NewsResponse(
        entity.getId(),
        entity.getTitle(),
        entity.getSummary(),
        entity.getSourceName(),
        entity.getSourceUrl(),
        entity.getPublishedAt(),
        entity.getCategory());
  }
}